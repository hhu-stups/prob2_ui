package de.prob2.ui.beditor;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import com.google.inject.Inject;

import de.prob.animator.domainobjects.ErrorItem;
import de.prob.scripting.ClassicalBFactory;
import de.prob.scripting.EventBFactory;
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
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.stage.Popup;

import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.event.MouseOverTextEvent;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@FXMLInjected
public class BEditor extends CodeArea {
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
		undoItem.setOnAction(e -> this.getUndoManager().undo());
		contextMenu.getItems().add(undoItem);

		final MenuItem redoItem = new MenuItem(bundle.getString("common.contextMenu.redo"));
		redoItem.setOnAction(e -> this.getUndoManager().redo());
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
	}

	private static <T> Collection<T> combineCollections(final Collection<T> a, final Collection<T> b) {
		final Collection<T> ret = new ArrayList<>(a);
		ret.addAll(b);
		return ret;
	}

	private int errorLocationAbsoluteStart(final ErrorItem.Location location) {
		return this.getAbsolutePosition(location.getStartLine() - 1, location.getStartColumn());
	}

	private int errorLocationAbsoluteEnd(final ErrorItem.Location location) {
		if (location.getStartLine() == location.getEndLine()) {
			final int displayedEndColumn = location.getStartColumn() == location.getEndColumn() ? location.getStartColumn() + 1 : location.getEndColumn();
			return this.getAbsolutePosition(location.getStartLine() - 1, displayedEndColumn);
		} else {
			return this.getAbsolutePosition(location.getEndLine() - 1, location.getEndColumn());
		}
	}

	private StyleSpans<Collection<String>> addErrorHighlighting(final StyleSpans<Collection<String>> highlighting) {
		StyleSpans<Collection<String>> highlightingWithErrors = highlighting;
		for (final ErrorItem error : this.getErrors()) {
			for (final ErrorItem.Location location : error.getLocations()) {
				final int startIndex = Math.min(this.errorLocationAbsoluteStart(location), this.getLength());
				final int endIndex = Math.min(this.errorLocationAbsoluteEnd(location), this.getLength());
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
		if (modelFactoryClass == ClassicalBFactory.class || modelFactoryClass == EventBFactory.class) {
			return BLexerSyntaxHighlighting.computeBHighlighting(text);
		} else if (RegexSyntaxHighlighting.canHighlight(modelFactoryClass)) {
			return RegexSyntaxHighlighting.computeHighlighting(modelFactoryClass, text);
		} else {
			// Do not highlight unknown languages.
			return StyleSpans.singleton(Collections.emptySet(), text.length());
		}
	}

	public void clearHistory() {
		this.getUndoManager().forgetHistory();
	}

	public ObservableList<ErrorItem> getErrors() {
		return this.errors;
	}
}
