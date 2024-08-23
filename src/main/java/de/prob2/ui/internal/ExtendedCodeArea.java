package de.prob2.ui.internal;

import java.time.Duration;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import com.google.common.base.Stopwatch;
import com.google.inject.Inject;

import de.prob.animator.domainobjects.ErrorItem;
import de.prob2.ui.codecompletion.CodeCompletionItem;
import de.prob2.ui.codecompletion.ParentWithEditableText;
import de.prob2.ui.internal.executor.BackgroundUpdater;
import de.prob2.ui.layout.FontSize;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
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
import org.fxmisc.richtext.model.Paragraph;
import org.fxmisc.richtext.model.StyleSpan;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CodeArea with error highlighting support.
 */
@FXMLInjected
public class ExtendedCodeArea extends CodeArea implements Builder<ExtendedCodeArea> {

	private static final Logger LOGGER = LoggerFactory.getLogger(ExtendedCodeArea.class);
	protected static final Map<ErrorItem.Type, String> ERROR_STYLE_CLASSES = Map.of(
			ErrorItem.Type.MESSAGE, "message",
			ErrorItem.Type.WARNING, "warning",
			ErrorItem.Type.ERROR, "error",
			ErrorItem.Type.INTERNAL_ERROR, "error"
	);
	private static final int MAX_TEXT_LENGTH_FOR_STYLING = 100_000;
	private static final int MAX_PAR_LENGTH_FOR_STYLING = 100_000;

	protected final FontSize fontSize;
	protected final I18n i18n;
	protected final ObservableList<ErrorItem> errors;
	protected final Popup errorPopup;
	protected final Label errorPopupLabel;
	protected final SimpleObjectProperty<ErrorItem.Location> errorHighlight;
	protected final SimpleObjectProperty<ErrorItem.Location> searchResult;
	private final BackgroundUpdater executor;
	private final ObservableValue<Optional<Point2D>> caretPos;
	private final ObservableValue<Optional<String>> textBeforeCaret;
	private final AtomicBoolean changingText;

	private boolean showLongTextWarning = true;

	@Inject
	public ExtendedCodeArea(FontSize fontSize, I18n i18n, StopActions stopActions) {
		this.fontSize = fontSize;
		this.i18n = i18n;
		this.executor = new BackgroundUpdater("ExtendedCodeArea Highlighting Updater");
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

	protected static Collection<String> combineStyleSpans(final Collection<String> a, final Collection<String> b) {
		// we want to save memory here!
		if (b.isEmpty() || a.equals(b)) {
			return a;
		} else if (a.isEmpty()) {
			return b;
		} else {
			HashSet<String> coll = new HashSet<>(a);
			coll.addAll(b);
			return coll;
		}
	}

	private void initialize() {
		this.getStyleClass().add("editor");
		styleProperty().bind(Bindings.format(Locale.ROOT, "-fx-font-size: %dpx;", fontSize.fontSizeProperty()));
		this.errorPopupLabel.getStyleClass().add("editorPopupLabel");

		if (showLineNumbers()) {
			this.setParagraphGraphicFactory(LineNumberFactory.get(this));
		}

		this.multiPlainChanges().subscribe(e -> this.reloadHighlighting());
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
					String errorStyleClass;
					if (error.getType() == null) {
						errorStyleClass = "message";
					} else {
						errorStyleClass = ERROR_STYLE_CLASSES.getOrDefault(error.getType(), "message");
					}

					highlighting = highlighting.overlay(
						new StyleSpansBuilder<Collection<String>>()
								.add(Set.of(), startIndex)
								.add(Set.of("problem", errorStyleClass), endIndex - startIndex)
							.create(),
							ExtendedCodeArea::combineStyleSpans
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
							.add(Set.of(), startIndex)
							.add(Set.of("errorTable"), endIndex - startIndex)
						.create(),
						ExtendedCodeArea::combineStyleSpans
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
							.add(Set.of(), startIndex)
							.add(Set.of("searchResult"), endIndex - startIndex)
						.create(),
						ExtendedCodeArea::combineStyleSpans
				);
			}
		}

		return highlighting;
	}

	private boolean checkTextLengthForStyling() {
		int length = this.getLength();
		// first check if text is too long, then if there are too long paragraphs (otherwise long text with short pars is ok(?))
		if (length >= MAX_TEXT_LENGTH_FOR_STYLING) {
			int maxParLength = this.getParagraphs().stream().mapToInt(Paragraph::length).max().orElse(0);
			if (maxParLength >= MAX_PAR_LENGTH_FOR_STYLING) {
				if (this.showLongTextWarning) {
					LOGGER.warn("Disabling styling in text area, paragraph/text is too long ({}/{})", maxParLength, length);
					this.showLongTextWarning = false;
				}
				return false;
			}
		}

		return true;
	}

	protected StyleSpans<Collection<String>> computeHighlighting(String text) {
		StyleSpans<Collection<String>> highlighting = StyleSpans.singleton(Set.of(), text.length());
		highlighting = addErrorHighlighting(highlighting);
		highlighting = addSearchHighlighting(highlighting);
		return highlighting;
	}

	public void reloadHighlighting() {
		String text = this.getText();
		if (!this.checkTextLengthForStyling()) {
			this.setStyleSpans(0, StyleSpans.singleton(new StyleSpan<>(Set.of(), text.length())));
			return;
		}

		this.executor.execute(() -> {
			try {
				// let the highlighting thread sleep for a bit, so we do not rapid fire updates when typing a lot
				Thread.sleep(50);
			} catch (InterruptedException ignored) {
				return;
			}

			Stopwatch sw = Stopwatch.createStarted();
			var spans = this.computeHighlighting(text);
			if (spans == null || Thread.currentThread().isInterrupted()) {
				return;
			}
			LOGGER.trace("Computing highlighting for text of length {} took {}", text.length(), sw.stop());

			Platform.runLater(() -> {
				// sanity check: did the text length change?
				if (this.getLength() == text.length()) {
					Stopwatch sw_ = Stopwatch.createStarted();
					try {
						this.setStyleSpans(0, spans);
						LOGGER.trace("Applying {} highlighting style spans took {}", spans.getSpanCount(), sw_.stop());
					} catch (Exception exc) {
						LOGGER.warn("Could not apply highlighting style spans", exc);
					}
				}
			});
		});
	}

	public void cancelHighlighting() {
		this.executor.cancel(true);
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
