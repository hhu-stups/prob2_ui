package de.prob2.ui.internal;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import com.google.inject.Inject;

import de.prob.animator.domainobjects.ErrorItem;
import de.prob2.ui.codecompletion.CodeCompletionItem;
import de.prob2.ui.codecompletion.ParentWithEditableText;
import de.prob2.ui.layout.FontSize;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.Builder;

import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.event.MouseOverTextEvent;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CodeArea with error highlighting support.
 */
@FXMLInjected
public class ExtendedCodeArea extends CodeArea implements Builder<ExtendedCodeArea> {

	protected static final Map<ErrorItem.Type, String> ERROR_STYLE_CLASSES;
	private static final Logger LOGGER = LoggerFactory.getLogger(ExtendedCodeArea.class);
	private static final int MAX_TEXT_LENGTH_FOR_STYLING = 100_000;

	static {
		final Map<ErrorItem.Type, String> errorStyleClasses = new EnumMap<>(ErrorItem.Type.class);
		errorStyleClasses.put(ErrorItem.Type.WARNING, "warning");
		errorStyleClasses.put(ErrorItem.Type.ERROR, "error");
		errorStyleClasses.put(ErrorItem.Type.INTERNAL_ERROR, "error");
		ERROR_STYLE_CLASSES = Collections.unmodifiableMap(errorStyleClasses);
	}

	protected final FontSize fontSize;
	protected final I18n i18n;
	protected final ObservableList<ErrorItem> errors;
	protected final Popup errorPopup;
	protected final Label errorPopupLabel;
	protected final SimpleObjectProperty<ErrorItem.Location> errorHighlight;
	protected final SimpleObjectProperty<ErrorItem.Location> searchResult;
	private final ExecutorService executor;
	private final ObservableValue<Optional<Point2D>> caretPos;
	private final ObservableValue<Optional<String>> textBeforeCaret;
	private final AtomicBoolean changingText;

	private boolean showLongTextWarning = true;

	@Inject
	public ExtendedCodeArea(FontSize fontSize, I18n i18n, StopActions stopActions) {
		this.fontSize = fontSize;
		this.i18n = i18n;
		this.executor = Executors.newSingleThreadExecutor();
		stopActions.add(this.executor::shutdownNow);

		this.errors = FXCollections.observableArrayList();
		this.errorPopup = new Popup();
		this.errorPopupLabel = new Label();
		this.errorPopup.getContent().add(this.errorPopupLabel);

		this.errorHighlight = new SimpleObjectProperty<>(null, "errorHighlight", null);
		this.searchResult = new SimpleObjectProperty<>(null, "searchResult", null);

		this.caretPos = Bindings.createObjectBinding(
			() -> this.caretBoundsProperty().getValue()
				      .map(bounds -> new Point2D(
					      (bounds.getMinX() + bounds.getMaxX()) / 2.0,
					      bounds.getMaxY()
				      )),
			this.caretBoundsProperty()
		);
		this.textBeforeCaret = Bindings.createObjectBinding(() -> {
			int caret = this.getCaretPosition();
			if (caret < 0 || caret > this.getLength()) {
				return Optional.empty();
			} else {
				return Optional.of(this.getText(0, caret));
			}
		}, this.textProperty(), this.caretPositionProperty());

		this.changingText = new AtomicBoolean();

		initialize();
	}

	protected static <T> Collection<T> combineCollections(final Collection<? extends T> a, final Collection<? extends T> b) {
		final Collection<T> ret = new ArrayList<>(a);
		ret.addAll(b);
		return ret;
	}

	private void initialize() {
		this.getStyleClass().add("editor");
		styleProperty().bind(Bindings.format(Locale.ROOT, "-fx-font-size: %dpx;", fontSize.fontSizeProperty()));
		this.errorPopupLabel.getStyleClass().add("editorPopupLabel");

		if (showLineNumbers()) {
			this.setParagraphGraphicFactory(LineNumberFactory.get(this));
		}

		this.multiPlainChanges()
			.successionEnds(Duration.ofMillis(50))
			.retainLatestUntilLater(this.executor)
			.filter(x -> !this.isChangingText() && checkTextLengthForStyling())
			.supplyTask(this::computeHighlightingAsync)
			.awaitLatest(this.multiPlainChanges())
			.filterMap(t -> {
				if (t.isSuccess()) {
					return Optional.of(t.get());
				} else {
					LOGGER.warn("Highlighting failed", t.getFailure());
					return Optional.empty();
				}
			})
			.subscribe(this::applyHighlighting);
		this.errors.addListener((ListChangeListener<ErrorItem>) change -> {
			if (this.errors.isEmpty()) {
				this.setErrorHighlight(null);
			}

			this.reloadHighlighting();
		});
		this.errorHighlight.addListener((observable, oldValue, newValue) -> this.reloadHighlighting());
		this.searchResult.addListener((observable, oldValue, newValue) -> this.reloadHighlighting());

		this.setMouseOverTextDelay(Duration.ofMillis(500));
		this.addEventHandler(MouseOverTextEvent.MOUSE_OVER_TEXT_BEGIN, e -> {
			// Inefficient, but works - there should never be so many errors that the iteration has a noticeable performance impact.
			final String errorsText = this.getErrors().stream()
				                          .filter(error ->
					                                  error.getLocations().stream()
						                                  .anyMatch(
							                                  location -> e.getCharacterIndex() >= this.errorLocationAbsoluteStart(location)
								                                              && e.getCharacterIndex() <= this.errorLocationAbsoluteEnd(location)
						                                  )
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

		initializeContextMenu();
	}

	protected boolean showLineNumbers() {
		return false;
	}

	private void initializeContextMenu() {
		final ContextMenu contextMenu = new ContextMenu();

		final MenuItem undoItem = new MenuItem(i18n.translate("common.contextMenu.undo"));
		undoItem.setOnAction(e -> this.getUndoManager().undo());
		contextMenu.getItems().add(undoItem);

		final MenuItem redoItem = new MenuItem(i18n.translate("common.contextMenu.redo"));
		redoItem.setOnAction(e -> this.getUndoManager().redo());
		contextMenu.getItems().add(redoItem);

		final MenuItem cutItem = new MenuItem(i18n.translate("common.contextMenu.cut"));
		cutItem.setOnAction(e -> this.cut());
		contextMenu.getItems().add(cutItem);

		final MenuItem copyItem = new MenuItem(i18n.translate("common.contextMenu.copy"));
		copyItem.setOnAction(e -> this.copy());
		contextMenu.getItems().add(copyItem);

		final MenuItem pasteItem = new MenuItem(i18n.translate("common.contextMenu.paste"));
		pasteItem.setOnAction(e -> this.paste());
		contextMenu.getItems().add(pasteItem);

		final MenuItem deleteItem = new MenuItem(i18n.translate("common.contextMenu.delete"));
		deleteItem.setOnAction(e -> this.deleteText(this.getSelection()));
		contextMenu.getItems().add(deleteItem);

		final MenuItem selectAllItem = new MenuItem(i18n.translate("common.contextMenu.selectAll"));
		selectAllItem.setOnAction(e -> this.selectAll());
		contextMenu.getItems().add(selectAllItem);

		this.setContextMenu(contextMenu);
	}

	@Override
	protected Predicate<Collection<String>> getFoldStyleCheck() {
		return null;
	}

	@Override
	protected UnaryOperator<Collection<String>> getAddFoldStyle() {
		return null;
	}

	@Override
	protected UnaryOperator<Collection<String>> getRemoveFoldStyle() {
		return null;
	}

	private int getClampedAbsolutePosition(int paragraphIndex, int columnIndex) {
		if (paragraphIndex < 0) {
			return 0;
		} else if (paragraphIndex >= this.getParagraphs().size()) {
			return this.getLength();
		}

		if (columnIndex < 0) {
			columnIndex = 0;
		} else if (columnIndex > this.getParagraphLength(paragraphIndex)) {
			columnIndex = this.getParagraphLength(paragraphIndex);
		}

		// let us not trust the library...
		Position clampedPos = this.getContent().position(paragraphIndex, columnIndex);
		return Math.max(0, Math.min(clampedPos.toOffset(), this.getLength()));
	}

	private int errorLocationAbsoluteStart(final ErrorItem.Location location) {
		Objects.requireNonNull(location, "location");
		if (location.getStartLine() > location.getEndLine()) {
			throw new IllegalArgumentException("line");
		} else if (location.getStartLine() == location.getEndLine()
			           && location.getStartColumn() > location.getEndColumn()) {
			throw new IllegalArgumentException("column");
		}

		int displayedStartColumn;
		if (
			location.getStartLine() == location.getEndLine()
				&& location.getStartColumn() == location.getEndColumn()
				&& location.getStartLine() >= 1
				&& location.getStartLine() <= this.getParagraphs().size()
				&& location.getStartColumn() >= this.getParagraphLength(location.getStartLine() - 1)
		) {
			// the styling cannot be displayed on substrings with length 0
			// thus - when we detect a location with length 0 - we extend it to length 1 to the right
			// but only if there is space, else extend to the left
			displayedStartColumn = location.getStartColumn() - 1;
		} else {
			displayedStartColumn = location.getStartColumn();
		}

		return this.getClampedAbsolutePosition(
			location.getStartLine() - 1,
			displayedStartColumn
		);
	}

	private int errorLocationAbsoluteEnd(final ErrorItem.Location location) {
		int displayedEndColumn;
		if (
			location.getStartLine() == location.getEndLine()
				&& location.getStartColumn() == location.getEndColumn()
				&& location.getEndLine() >= 1
				&& location.getEndLine() <= this.getParagraphs().size()
				&& location.getEndColumn() < this.getParagraphLength(location.getStartLine() - 1)
		) {
			// the styling cannot be displayed on substrings with length 0
			// thus - when we detect a location with length 0 - we extend it to length 1 to the right
			// but only if there is space, else extend to the left
			displayedEndColumn = location.getEndColumn() + 1;
		} else {
			displayedEndColumn = location.getEndColumn();
		}

		return this.getClampedAbsolutePosition(
			location.getEndLine() - 1,
			displayedEndColumn
		);
	}

	public void jumpToErrorSource(ErrorItem.Location errorLocation) {
		this.setErrorHighlight(errorLocation);
		this.moveTo(this.getClampedAbsolutePosition(
			errorLocation.getStartLine() - 1,
			errorLocation.getStartColumn()
		));
		this.requestFollowCaret();
	}

	protected StyleSpans<Collection<String>> addErrorHighlighting(StyleSpans<Collection<String>> highlighting) {
		for (ErrorItem error : this.getErrors()) {
			for (ErrorItem.Location location : error.getLocations()) {
				int startIndex = this.errorLocationAbsoluteStart(location);
				int endIndex = this.errorLocationAbsoluteEnd(location);
				if (endIndex > startIndex) {
					highlighting = highlighting.overlay(
						new StyleSpansBuilder<Collection<String>>()
							.add(Collections.emptySet(), startIndex)
							.add(Arrays.asList("problem", ERROR_STYLE_CLASSES.get(error.getType())), endIndex - startIndex)
							.create(),
						ExtendedCodeArea::combineCollections
					);
				}
			}
		}

		if (errorHighlight.get() != null) {
			int startIndex = this.errorLocationAbsoluteStart(errorHighlight.get());
			int endIndex = this.errorLocationAbsoluteEnd(errorHighlight.get());
			if (endIndex > startIndex) {
				highlighting = highlighting.overlay(
					new StyleSpansBuilder<Collection<String>>()
						.add(Collections.emptySet(), startIndex)
						.add(Collections.singletonList("errorTable"), endIndex - startIndex)
						.create(),
					ExtendedCodeArea::combineCollections
				);
			}
		}

		return highlighting;
	}

	public void jumpToSearchResult(ErrorItem.Location searchResultLocation) {
		this.moveTo(this.getClampedAbsolutePosition(
			searchResultLocation.getEndLine() - 1,
			searchResultLocation.getEndColumn()
		));
		this.requestFollowCaret();
		this.setSearchResult(searchResultLocation);
	}

	protected StyleSpans<Collection<String>> addSearchHighlighting(StyleSpans<Collection<String>> highlighting) {
		if (searchResult.get() != null) {
			int startIndex = this.errorLocationAbsoluteStart(searchResult.get());
			int endIndex = this.errorLocationAbsoluteEnd(searchResult.get());
			if (endIndex > startIndex) {
				highlighting = highlighting.overlay(
					new StyleSpansBuilder<Collection<String>>()
						.add(Collections.emptySet(), startIndex)
						.add(Collections.singletonList("searchResult"), endIndex - startIndex)
						.create(),
					ExtendedCodeArea::combineCollections
				);
			}
		}

		return highlighting;
	}

	private void applyHighlighting(Optional<StyleSpans<Collection<String>>> highlighting) {
		highlighting.ifPresent(styleSpans -> this.setStyleSpans(0, styleSpans));
	}

	private Task<Optional<StyleSpans<Collection<String>>>> computeHighlightingAsync() {
		final String text = this.getText();
		final Task<Optional<StyleSpans<Collection<String>>>> task = new Task<>() {

			@Override
			protected Optional<StyleSpans<Collection<String>>> call() {
				return computeHighlighting(text);
			}
		};
		executor.execute(task);
		return task;
	}

	private boolean checkTextLengthForStyling() {
		int length = this.getLength();
		if (length >= MAX_TEXT_LENGTH_FOR_STYLING) {
			if (this.showLongTextWarning) {
				LOGGER.warn("Disabling styling in text area, text is too long ({})", length);
				this.showLongTextWarning = false;
			}

			return false;
		}

		return true;
	}

	protected Optional<StyleSpans<Collection<String>>> computeHighlighting(String text) {
		if (!checkTextLengthForStyling()) {
			return Optional.empty();
		}

		StyleSpans<Collection<String>> highlighting = StyleSpans.singleton(Collections.emptySet(), text.length());
		highlighting = addErrorHighlighting(highlighting);
		highlighting = addSearchHighlighting(highlighting);
		return Optional.of(highlighting);
	}

	public void reloadHighlighting() {
		if (!checkTextLengthForStyling()) {
			return;
		}

		this.applyHighlighting(computeHighlighting(this.getText()));
	}

	public void clearHistory() {
		this.getUndoManager().forgetHistory();
	}

	public ObservableList<ErrorItem> getErrors() {
		return this.errors;
	}

	public void setErrorHighlight(ErrorItem.Location errorLocation) {
		this.errorHighlight.set(errorLocation);
	}

	public void setSearchResult(ErrorItem.Location searchResultLocation) {
		this.searchResult.set(searchResultLocation);
	}

	@Override
	public ExtendedCodeArea build() {
		return this;
	}

	public ObservableValue<Optional<Point2D>> caretPosProperty() {
		return caretPos;
	}

	public ObservableValue<Optional<String>> textBeforeCaretProperty() {
		return textBeforeCaret;
	}

	public boolean isChangingText() {
		return this.changingText.get();
	}

	public void setChangingText(boolean changingText) {
		this.changingText.set(changingText);
		if (changingText) {
			this.showLongTextWarning = true;
		}
	}

	protected abstract class AbstractParentWithEditableText<T extends CodeCompletionItem> implements ParentWithEditableText<T> {

		@Override
		public Window getWindow() {
			return ExtendedCodeArea.this.getScene().getWindow();
		}

		@Override
		public ObservableValue<Optional<Point2D>> getCaretPosition() {
			return ExtendedCodeArea.this.caretPosProperty();
		}

		@Override
		public ObservableValue<Optional<String>> getTextBeforeCaret() {
			return ExtendedCodeArea.this.textBeforeCaretProperty();
		}
	}
}
