package de.prob2.ui.beditor;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob.animator.domainobjects.ErrorItem;
import de.prob.scripting.ClassicalBFactory;
import de.prob.scripting.EventBFactory;
import de.prob.scripting.EventBPackageFactory;
import de.prob.scripting.ModelFactory;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StopActions;
import de.prob2.ui.layout.FontSize;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.Machine;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Bounds;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.IndexRange;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.stage.Popup;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.event.MouseOverTextEvent;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.fxmisc.wellbehaved.event.EventPattern;
import org.fxmisc.wellbehaved.event.InputMap;
import org.fxmisc.wellbehaved.event.Nodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Singleton
@FXMLInjected
public class BEditor extends CodeArea {

	private static final Set<KeyCode> REST = EnumSet.of(KeyCode.ESCAPE, KeyCode.SCROLL_LOCK, KeyCode.PAUSE, KeyCode.NUM_LOCK, KeyCode.INSERT, KeyCode.CONTEXT_MENU, KeyCode.CAPS);

	private static final Logger LOGGER = LoggerFactory.getLogger(BEditor.class);

	private static final Map<ErrorItem.Type, String> ERROR_STYLE_CLASSES;
	static {
		final Map<ErrorItem.Type, String> errorStyleClasses = new EnumMap<>(ErrorItem.Type.class);
		errorStyleClasses.put(ErrorItem.Type.WARNING, "warning");
		errorStyleClasses.put(ErrorItem.Type.ERROR, "error");
		errorStyleClasses.put(ErrorItem.Type.INTERNAL_ERROR, "error");
		ERROR_STYLE_CLASSES = Collections.unmodifiableMap(errorStyleClasses);
	}

	private final FontSize fontSize;
	private final CurrentProject currentProject;
	private final ResourceBundle bundle;

	private final ExecutorService executor;
	private final ObservableList<ErrorItem> errors;
	private final Popup errorPopup;
	private final Label errorPopupLabel;

	@Inject
	private BEditor(final FontSize fontSize, final ResourceBundle bundle, final CurrentProject currentProject, final StopActions stopActions) {
		this.fontSize = fontSize;
		this.currentProject = currentProject;
		this.bundle = bundle;
		this.executor = Executors.newSingleThreadExecutor();
		stopActions.add(this.executor::shutdownNow);
		this.errors = FXCollections.observableArrayList();
		this.errorPopup = new Popup();
		this.errorPopupLabel = new Label();
		this.errorPopup.getContent().add(this.errorPopupLabel);
		initialize();
		initializeContextMenu();
	}

	private void initializeContextMenu() {
		final ContextMenu contextMenu = new ContextMenu();

		final MenuItem undoItem = new MenuItem(bundle.getString("common.contextMenu.undo"));
		undoItem.setOnAction(e -> this.undo());
		contextMenu.getItems().add(undoItem);

		final MenuItem redoItem = new MenuItem(bundle.getString("common.contextMenu.redo"));
		redoItem.setOnAction(e -> this.redo());
		contextMenu.getItems().add(redoItem);

		final MenuItem cutItem = new MenuItem(bundle.getString("common.contextMenu.cut"));
		cutItem.setOnAction(e -> this.cut());
		contextMenu.getItems().add(cutItem);

		final MenuItem copyItem = new MenuItem(bundle.getString("common.contextMenu.copy"));
		copyItem.setOnAction(e -> this.copy());
		contextMenu.getItems().add(copyItem);

		final MenuItem pasteItem = new MenuItem(bundle.getString("common.contextMenu.paste"));
		pasteItem.setOnAction(e -> this.paste());
		contextMenu.getItems().add(pasteItem);

		final MenuItem deleteItem = new MenuItem(bundle.getString("common.contextMenu.delete"));
		deleteItem.setOnAction(e -> this.deleteText(this.getSelection()));
		contextMenu.getItems().add(deleteItem);

		final MenuItem selectAllItem = new MenuItem(bundle.getString("common.contextMenu.selectAll"));
		selectAllItem.setOnAction(e -> this.selectAll());
		contextMenu.getItems().add(selectAllItem);

		this.setContextMenu(contextMenu);
	}

	private void initialize() {
		this.getStyleClass().add("editor");
		this.setParagraphGraphicFactory(LineNumberFactory.get(this));
		this.richChanges()
				.filter(ch -> !ch.isPlainTextIdentity())
				.successionEnds(Duration.ofMillis(100))
				.supplyTask(this::computeHighlightingAsync)
				.awaitLatest(this.richChanges())
				.filterMap(t -> {
					if (t.isSuccess()) {
						return Optional.of(t.get());
					} else {
						LOGGER.info("Highlighting failed", t.getFailure());
						return Optional.empty();
					}
				}).subscribe(this::applyHighlighting);
		this.getErrors().addListener((ListChangeListener<ErrorItem>) change ->
				this.applyHighlighting(computeHighlighting(this.getText(), currentProject.getCurrentMachine()))
		);

		this.errorPopupLabel.getStyleClass().add("editorPopupLabel");

		this.setMouseOverTextDelay(Duration.ofMillis(500));
		this.addEventHandler(MouseOverTextEvent.MOUSE_OVER_TEXT_BEGIN, e -> {
			// Inefficient, but works - there should never be so many errors that the iteration has a noticeable performance impact.
			final String errorsText = this.getErrors().stream()
				.filter(error -> error.getLocations().stream().anyMatch(location ->
					e.getCharacterIndex() >= this.errorLocationAbsoluteStart(location)
						&& e.getCharacterIndex() <= this.errorLocationAbsoluteEnd(location))
				)
				.map(ErrorItem::getMessage)
				.collect(Collectors.joining("\n"));
			if (!errorsText.isEmpty()) {
				this.errorPopupLabel.setText(errorsText);
				// Try to position the popup under the text being hovered over,
				// so that the line in question is not covered by the popup.
				final double popupY = this.getCharacterBoundsOnScreen(e.getCharacterIndex(), e.getCharacterIndex() + 1)
					.map(Bounds::getMaxY)
					.orElse(e.getScreenPosition().getY());
				this.errorPopup.show(this, e.getScreenPosition().getX(), popupY);
			}
		});
		this.addEventHandler(MouseOverTextEvent.MOUSE_OVER_TEXT_END, e -> this.errorPopup.hide());

		fontSize.fontSizeProperty().addListener((observable, from, to) ->
				this.setStyle(String.format("-fx-font-size: %dpx;", to.intValue()))
		);

		Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(), this::keyPressed));
		Nodes.addInputMap(this, InputMap.consume(EventPattern.keyTyped(), this::keyTyped));

		// GUI-style shortcuts, these should use the Shortcut key (i. e. Command on Mac, Control on other systems).
		Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(KeyCode.C, KeyCombination.SHORTCUT_DOWN), e -> this.copy()));
		Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(KeyCode.V, KeyCombination.SHORTCUT_DOWN), e-> this.paste()));
		Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(KeyCode.X, KeyCombination.SHORTCUT_DOWN), e-> this.cut()));
		Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(KeyCode.A, KeyCombination.SHORTCUT_DOWN), e-> this.selectAll()));

		// Do not handle undo and redo here as KeyCode.Z is confused with KeyCode.Y on German keyboard

		Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(KeyCode.DELETE), e -> this.handleDelete()));
		Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(KeyCode.BACK_SPACE), e -> this.handleBackspace()));

		Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(KeyCode.UP, KeyCombination.SHIFT_DOWN), this::handleUp));
		Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(KeyCode.DOWN, KeyCombination.SHIFT_DOWN), this::handleDown));
		Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(KeyCode.LEFT, KeyCombination.SHIFT_DOWN), this::handleLeft));
		Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(KeyCode.RIGHT, KeyCombination.SHIFT_DOWN), this::handleRight));

		Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(KeyCode.UP), this::handleUp));
		Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(KeyCode.DOWN), this::handleDown));
		Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(KeyCode.LEFT), this::handleLeft));
		Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(KeyCode.RIGHT), this::handleRight));

		Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(KeyCode.UP), this::handleUp));
		Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(KeyCode.DOWN), this::handleDown));
		Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(KeyCode.LEFT), this::handleLeft));
		Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(KeyCode.RIGHT), this::handleRight));

	}

	protected boolean handleUndoRedo(KeyEvent e) {
		boolean zIsPressed = e.getText().equals("Z") || e.getText().equals("z");
		if(zIsPressed) {
			if(e.isShortcutDown()) {
				if (e.isShiftDown()) {
					redo();
					return true;
				} else {
					undo();
					return true;
				}
			}
		}
		return false;
	}

	protected void keyTyped(KeyEvent e) {
		//Handle this key pressed. Otherwise chars cannot be handled which are typed in using Shortcut or Control
	}

	protected void keyPressed(KeyEvent e) {
		if(handleUndoRedo(e)) {
			return;
		}
		if (REST.contains(e.getCode())) {
			return;
		}
		if (!e.getCode().isFunctionKey() && !e.getCode().isMediaKey() && !e.getCode().isModifierKey() &&
			!e.isControlDown() && !e.isMetaDown() && !e.getText().isEmpty()) {
			if(!this.getSelectedText().isEmpty()) {
				this.deleteText(this.getSelection());
			}
			this.insertText(this.getCaretPosition(), e.getText());
		}
	}

	private static <T> Collection<T> combineCollections(final Collection<T> a, final Collection<T> b) {
		final Collection<T> ret = new ArrayList<>(a);
		ret.addAll(b);
		return ret;
	}

	private int getClampedAbsolutePosition(final int paragraphIndex, final int columnIndex) {
		if (paragraphIndex >= this.getParagraphs().size() - 1) {
			return this.getLength();
		}
		return Math.min(this.getAbsolutePosition(paragraphIndex, columnIndex), this.getLength());
	}

	private int errorLocationAbsoluteStart(final ErrorItem.Location location) {
		return this.getClampedAbsolutePosition(location.getStartLine() - 1, location.getStartColumn());
	}

	private int errorLocationAbsoluteEnd(final ErrorItem.Location location) {
		if (location.getStartLine() == location.getEndLine()) {
			final int displayedEndColumn = location.getStartColumn() == location.getEndColumn() ? location.getStartColumn() + 1 : location.getEndColumn();
			return this.getClampedAbsolutePosition(location.getStartLine() - 1, displayedEndColumn);
		} else {
			return this.getClampedAbsolutePosition(location.getEndLine() - 1, location.getEndColumn());
		}
	}

	private StyleSpans<Collection<String>> addErrorHighlighting(final StyleSpans<Collection<String>> highlighting) {
		StyleSpans<Collection<String>> highlightingWithErrors = highlighting;
		for (final ErrorItem error : this.getErrors()) {
			for (final ErrorItem.Location location : error.getLocations()) {
				final int startIndex = this.errorLocationAbsoluteStart(location);
				final int endIndex = this.errorLocationAbsoluteEnd(location);
				highlightingWithErrors = highlightingWithErrors.overlay(
						new StyleSpansBuilder<Collection<String>>()
								.add(Collections.emptyList(), startIndex)
								.add(Arrays.asList("problem", ERROR_STYLE_CLASSES.get(error.getType())), endIndex - startIndex)
								.create(),
						BEditor::combineCollections
				);
			}
		}
		return highlightingWithErrors;
	}

	private void applyHighlighting(StyleSpans<Collection<String>> highlighting) {
		this.setStyleSpans(0, addErrorHighlighting(highlighting));
	}

	private Task<StyleSpans<Collection<String>>> computeHighlightingAsync() {
		final String text = this.getText();
		final Task<StyleSpans<Collection<String>>> task = new Task<StyleSpans<Collection<String>>>() {
			@Override
			protected StyleSpans<Collection<String>> call() {
				return computeHighlighting(text, currentProject.getCurrentMachine());
			}
		};
		executor.execute(task);
		return task;
	}

	private static StyleSpans<Collection<String>> computeHighlighting(String text, Machine machine) {
		if (machine == null) {
			//Prompt text is a comment text
			return StyleSpans.singleton(Collections.singleton("editor_comment"), text.length());
		}
		Class<? extends ModelFactory<?>> modelFactoryClass = machine.getModelFactoryClass();
		if (modelFactoryClass == ClassicalBFactory.class || modelFactoryClass == EventBFactory.class || modelFactoryClass == EventBPackageFactory.class) {
			return BLexerSyntaxHighlighting.computeBHighlighting(text);
		} else if (RegexSyntaxHighlighting.canHighlight(modelFactoryClass)) {
			return RegexSyntaxHighlighting.computeHighlighting(modelFactoryClass, text);
		} else {
			// Do not highlight unknown languages.
			return StyleSpans.singleton(Collections.emptySet(), text.length());
		}
	}

	protected void updateRangeUpLeft(KeyEvent e, int newCaret) {
		if(e.isShiftDown()) {
			IndexRange range = this.getSelection();
			if(newCaret < range.getStart()) {
				this.selectRange(range.getEnd(), newCaret);
			} else {
				this.selectRange(range.getStart(), newCaret);
			}
		} else {
			this.moveTo(newCaret);
		}
	}

	protected void updateRangeDownRight(KeyEvent e, int newCaret) {
		if(e.isShiftDown()) {
			IndexRange range = this.getSelection();
			if(newCaret > range.getEnd()) {
				this.selectRange(range.getStart(), newCaret);
			} else {
				this.selectRange(range.getEnd(), newCaret);
			}
		} else {
			this.moveTo(newCaret);
		}
	}

	protected void handleUp(KeyEvent e) {
		if(this.getCaretPosition() == 0) {
			return;
		}
		int lineNumber = Math.max(0, this.getCurrentParagraph() - 1);
		int newCaret = getAbsolutePosition(lineNumber, lineNumber == 0 ? 0 : Math.min(this.getParagraphs().get(lineNumber).length(), this.getCaretColumn()));
		if(!this.getSelectedText().isEmpty() && !e.isShiftDown()) {
			this.deselect();
			this.moveTo(newCaret);
		} else {
			updateRangeUpLeft(e, newCaret);
		}
		requestFollowCaret(); //This forces the text area to scroll. Other functions do not implement the require effects.
	}

	protected void handleDown(KeyEvent e) {
		if(this.getCaretPosition() == this.getText().length()) {
			return;
		}
		int lineNumber = Math.min(this.getParagraphs().size() - 1, this.getCurrentParagraph() + 1);
		int newCaret = getAbsolutePosition(lineNumber, lineNumber == this.getParagraphs().size() - 1 ? this.getParagraphs().get(lineNumber).length() : Math.min(this.getParagraphs().get(lineNumber).length(), this.getCaretColumn()));
		if(!this.getSelectedText().isEmpty() && !e.isShiftDown()) {
			this.deselect();
			this.moveTo(newCaret);
		} else {
			updateRangeDownRight(e, newCaret);
		}
		requestFollowCaret(); //This forces the text area to scroll. Other functions do not implement the require effects.
	}

	protected void handleLeft(KeyEvent e) {
		int caret = this.getCaretPosition();
		if(caret == 0) {
			return;
		}
		int newCaret = Math.max(0, caret - 1);
		if(!this.getSelectedText().isEmpty() && !e.isShiftDown()) {
			IndexRange range = this.getSelection();
			this.deselect();
			this.moveTo(Math.min(range.getStart(), range.getEnd()) - 1);
		} else {
			updateRangeUpLeft(e, newCaret);
		}
		requestFollowCaret(); //This forces the text area to scroll. Other functions do not implement the require effects.
	}

	protected void handleRight(KeyEvent e) {
		int caret = this.getCaretPosition();
		if(caret == this.getText().length()) {
			return;
		}
		int newCaret = Math.min(this.getText().length(), caret + 1);
		if(!this.getSelectedText().isEmpty() && !e.isShiftDown()) {
			IndexRange range = this.getSelection();
			this.deselect();
			this.moveTo(Math.max(range.getStart(), range.getEnd()) + 1);
		} else {
			updateRangeDownRight(e, newCaret);
		}
		requestFollowCaret(); //This forces the text area to scroll. Other functions do not implement the require effects.
	}

	protected void handleDelete() {
		if(this.getSelectedText().isEmpty()) {
			this.deleteNextChar();
		}	else {
			this.deleteText(this.getSelection());
		}
	}

	protected void handleBackspace() {
		if(this.getSelectedText().isEmpty()) {
			this.deletePreviousChar();
		}	else {
			this.deleteText(this.getSelection());
		}
	}

	public int getLineNumber() {
		return this.getParagraphs().size()-1;
	}

	public void undo() {
		this.getUndoManager().undo();
	}

	public void redo() {
		this.getUndoManager().redo();
	}

	public void clearHistory() {
		this.getUndoManager().forgetHistory();
	}

	public ObservableList<ErrorItem> getErrors() {
		return this.errors;
	}
}
