package de.prob2.ui.internal;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import javax.inject.Inject;

import de.prob.animator.domainobjects.ErrorItem;
import de.prob2.ui.layout.FontSize;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Bounds;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.stage.Popup;
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
	private final ExecutorService executor;

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

		this.richChanges()
				.filter(ch -> !ch.isPlainTextIdentity())
				.successionEnds(Duration.ofMillis(100))
				.supplyTask(this::computeHighlightingAsync)
				.awaitLatest(this.richChanges())
				.filterMap(t -> {
					if (t.isSuccess()) {
						return Optional.of(t.get());
					} else {
						LOGGER.warn("Highlighting failed", t.getFailure());
						return Optional.empty();
					}
				})
				.subscribe(this::applyHighlighting);
		this.errors.addListener((ListChangeListener<ErrorItem>) change -> this.reloadHighlighting());
		this.errorHighlight.addListener((observable, oldValue, newValue) -> this.reloadHighlighting());

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

	protected StyleSpans<Collection<String>> addErrorHighlighting(StyleSpans<Collection<String>> highlighting) {
		for (ErrorItem error : this.getErrors()) {
			for (ErrorItem.Location location : error.getLocations()) {
				int startIndex = this.errorLocationAbsoluteStart(location);
				int endIndex = this.errorLocationAbsoluteEnd(location);
				highlighting = highlighting.overlay(
						new StyleSpansBuilder<Collection<String>>()
								.add(Collections.emptySet(), startIndex)
								.add(Arrays.asList("problem", ERROR_STYLE_CLASSES.get(error.getType())), endIndex - startIndex)
								.create(),
						ExtendedCodeArea::combineCollections
				);
			}
		}

		if (errorHighlight.get() != null) {
			int startIndex = this.errorLocationAbsoluteStart(errorHighlight.get());
			int endIndex = this.errorLocationAbsoluteEnd(errorHighlight.get());
			highlighting = highlighting.overlay(
					new StyleSpansBuilder<Collection<String>>()
							.add(Collections.emptySet(), startIndex)
							.add(Collections.singletonList("errorTable"), endIndex - startIndex)
							.create(),
					ExtendedCodeArea::combineCollections
			);
		}

		return highlighting;
	}

	private void applyHighlighting(StyleSpans<Collection<String>> highlighting) {
		this.setStyleSpans(0, highlighting);
	}

	private Task<StyleSpans<Collection<String>>> computeHighlightingAsync() {
		final String text = this.getText();
		final Task<StyleSpans<Collection<String>>> task = new Task<StyleSpans<Collection<String>>>() {

			@Override
			protected StyleSpans<Collection<String>> call() {
				return computeHighlighting(text);
			}
		};
		executor.execute(task);
		return task;
	}

	protected StyleSpans<Collection<String>> computeHighlighting(String text) {
		StyleSpans<Collection<String>> highlighting = StyleSpans.singleton(Collections.emptySet(), text.length());
		highlighting = addErrorHighlighting(highlighting);
		return highlighting;
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

	public void reloadHighlighting() {
		this.applyHighlighting(computeHighlighting(this.getText()));
	}

	@Override
	public ExtendedCodeArea build() {
		return this;
	}
}
