package de.prob2.ui.consoles;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Collectors;

import de.prob2.ui.codecompletion.CodeCompletionItem;
import de.prob2.ui.codecompletion.ParentWithEditableText;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StringHelper;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.IntegerExpression;
import javafx.beans.binding.StringBinding;
import javafx.beans.binding.StringExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.IndexRange;
import javafx.scene.control.MenuItem;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.InputMethodEvent;
import javafx.scene.input.KeyCharacterCombination;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.TransferMode;
import javafx.stage.Window;

import org.controlsfx.tools.Platform;
import org.fxmisc.richtext.StyleClassedTextArea;
import org.fxmisc.wellbehaved.event.Nodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static javafx.scene.input.KeyCombination.CONTROL_DOWN;
import static javafx.scene.input.KeyCombination.SHIFT_DOWN;
import static javafx.scene.input.KeyCombination.SHORTCUT_DOWN;
import static org.fxmisc.wellbehaved.event.EventPattern.anyOf;
import static org.fxmisc.wellbehaved.event.EventPattern.keyPressed;
import static org.fxmisc.wellbehaved.event.EventPattern.keyTyped;
import static org.fxmisc.wellbehaved.event.EventPattern.mouseClicked;
import static org.fxmisc.wellbehaved.event.InputMap.consume;

public abstract class Console extends StyleClassedTextArea {

	private static final Logger LOGGER = LoggerFactory.getLogger(Console.class);

	private final I18n i18n;
	private final Executable interpreter;

	private final BooleanProperty lineContinuation;
	private final StringProperty header;
	private final StringProperty prompt;
	private final StringProperty lineContinuationPrompt;
	private final StringProperty successfulSearchPrompt;
	private final StringProperty failedSearchPrompt;
	private final StringProperty input;
	private final StringExpression inputWithPrompt;
	private final IntegerExpression inputStart;
	private final IntegerExpression inputEnd;
	private final ObservableValue<Optional<Point2D>> caretPos;
	private final ObservableValue<Optional<String>> textBeforeCaret;

	private final ConsoleHistoryAndSearchHandler historyAndSearchHandler;
	private final StringBuilder commandBuffer;
	private CachedMessage lastMessage;

	protected Console(I18n i18n, Executable interpreter, String header, String prompt) {
		this.i18n = Objects.requireNonNull(i18n, "i18n");
		this.interpreter = Objects.requireNonNull(interpreter, "interpreter");

		this.lineContinuation = new SimpleBooleanProperty(this, "lineContinuation", false);
		this.header = new SimpleStringProperty(this, "header", Objects.requireNonNull(header, "header"));
		this.prompt = new SimpleStringProperty(this, "prompt", Objects.requireNonNull(prompt, "prompt"));
		this.lineContinuationPrompt = new SimpleStringProperty(this, "lineContinuationPrompt", "consoles.prompt.lineContinuation");
		this.successfulSearchPrompt = new SimpleStringProperty(this, "successfulSearchPrompt", "consoles.prompt.backwardSearch");
		this.failedSearchPrompt = new SimpleStringProperty(this, "failedSearchPrompt", "consoles.prompt.backwardSearchFailed");
		this.input = new SimpleStringProperty(this, "input", "");

		this.historyAndSearchHandler = new ConsoleHistoryAndSearchHandler(this);
		this.commandBuffer = new StringBuilder();

		this.requestFollowCaret();
		initializeContextMenu();
		setEvents();
		setDragDrop();
		this.setWrapText(true);
		this.getStyleClass().add("console");

		StringBinding translatedPrompt = i18n.translateBinding(this.prompt);
		StringBinding translatedLineContinuationPrompt = i18n.translateBinding(this.lineContinuationPrompt);
		StringBinding translatedSuccessfulSearchPrompt = i18n.translateBinding(this.successfulSearchPrompt);
		StringBinding translatedFailedSearchPrompt = i18n.translateBinding(this.failedSearchPrompt);
		this.inputStart = Bindings.createIntegerBinding(() -> {
			if (historyAndSearchHandler.isSearchActive()) {
				if (historyAndSearchHandler.isSearchFailed()) {
					return translatedFailedSearchPrompt.get().length() + 1;
				} else {
					return translatedSuccessfulSearchPrompt.get().length() + 1;
				}
			} else {
				if (lineContinuation.get()) {
					return translatedLineContinuationPrompt.get().length();
				} else {
					return translatedPrompt.get().length();
				}
			}
		}, this.lineContinuation, translatedPrompt, translatedLineContinuationPrompt, translatedSuccessfulSearchPrompt, translatedFailedSearchPrompt, this.input, this.historyAndSearchHandler.searchActiveProperty(), this.historyAndSearchHandler.searchFailedProperty());
		this.inputEnd = Bindings.createIntegerBinding(() -> this.inputStart.get() + this.input.get().length(), this.inputStart, this.input);
		this.inputWithPrompt = Bindings.createStringBinding(() -> {
			if (historyAndSearchHandler.isSearchActive()) {
				String result = historyAndSearchHandler.getCurrentSearchResult();
				if (historyAndSearchHandler.isSearchFailed()) {
					return translatedFailedSearchPrompt.get() + "`" + input.get() + "': " + result;
				} else {
					return translatedSuccessfulSearchPrompt.get() + "`" + input.get() + "': " + result;
				}
			} else {
				if (lineContinuation.get()) {
					return translatedLineContinuationPrompt.get() + input.get();
				} else {
					return translatedPrompt.get() + input.get();
				}
			}
		}, this.lineContinuation, translatedPrompt, translatedLineContinuationPrompt, translatedSuccessfulSearchPrompt, translatedFailedSearchPrompt, this.input, this.historyAndSearchHandler.searchActiveProperty(), this.historyAndSearchHandler.currentSearchResultProperty(), this.historyAndSearchHandler.searchFailedProperty());
		this.inputWithPrompt.addListener((o, from, to) -> this.update(from, to));

		this.caretPos = Bindings.createObjectBinding(
			() -> this.caretBoundsProperty().getValue()
				      .map(bounds -> new Point2D(
					      (bounds.getMinX() + bounds.getMaxX()) / 2.0,
					      bounds.getMaxY()
				      )),
			this.caretBoundsProperty()
		);
		this.textBeforeCaret = Bindings.createObjectBinding(() -> {
			OptionalInt positionInInput = this.getPositionInInput();
			if (positionInInput.isPresent()) {
				return Optional.of(this.getInput().substring(0, positionInInput.getAsInt()));
			} else {
				return Optional.empty();
			}
		}, this.inputProperty(), this.caretPositionProperty());

		this.reset();
	}

	private static boolean checkControlKeys(KeyEvent event) {
		if (event == null || event.getCharacter() == null) {
			return false;
		}

		// this code is copied from RichTextFX's GenericStyledAreaBehavior
		if (Platform.getCurrent() == Platform.WINDOWS) {
			if (event.isControlDown() && event.isAltDown() && !event.isMetaDown() && event.getCharacter().length() == 1 && event.getCharacter().getBytes()[0] != 0) {
				return true;
			} else {
				return !event.isControlDown() && !event.isAltDown() && !event.isMetaDown();
			}
		} else {
			return !event.isControlDown() && !event.isMetaDown();
		}
	}

	private void update() {
		this.update(null, this.inputWithPrompt.get());
	}

	private void update(String from, String to) {
		int caretParagraph = this.getCurrentParagraph();
		int caretColumn = this.getCaretColumn();

		boolean empty = this.getLength() == 0;
		if (empty) {
			this.insert(0, i18n.translate(this.header.get()) + "\n", Set.of("header"));
		}

		assert this.getParagraphs().size() >= 2; // always at least header + prompt
		int lastParagraph = this.getParagraphs().size() - 1;
		int paragraphLength = this.getParagraphLength(lastParagraph);
		this.replace(this.getAbsolutePosition(lastParagraph, 0), this.getAbsolutePosition(lastParagraph, paragraphLength), to, Set.of());

		if (empty || from == null) {
			this.moveTo(lastParagraph, this.inputEnd.get(), SelectionPolicy.CLEAR);
		} else {
			this.moveTo(caretParagraph, Math.max(0, Math.min(this.getParagraphLength(caretParagraph), caretColumn)), SelectionPolicy.CLEAR);
		}
	}

	private void initializeContextMenu() {
		final ContextMenu contextMenu = new ContextMenu();

		final MenuItem copyItem = new MenuItem(i18n.translate("common.contextMenu.copy"));
		copyItem.setOnAction(e -> this.copy());
		contextMenu.getItems().add(copyItem);

		final MenuItem pasteItem = new MenuItem(i18n.translate("common.contextMenu.paste"));
		pasteItem.setOnAction(e -> this.paste());
		contextMenu.getItems().add(pasteItem);

		final MenuItem clearItem = new MenuItem(i18n.translate("common.contextMenu.clear"));
		clearItem.setOnAction(e -> this.reset());
		contextMenu.getItems().add(clearItem);

		this.setContextMenu(contextMenu);
	}

	public void setEvents() {
		Nodes.addInputMap(this, consume(mouseClicked(MouseButton.PRIMARY)));
		Nodes.addInputMap(this, consume(keyPressed().onlyIf(e -> (e.getCode().isLetterKey() || e.getCode().isDigitKey() || e.getCode().isWhitespaceKey()) && checkControlKeys(e))));
		Nodes.addInputMap(this, consume(keyTyped().onlyIf(e -> checkControlKeys(e) && StringHelper.containsNoControlCharacters(e.getCharacter())), e -> this.onEnterText(e.getCharacter())));

		// GUI-style shortcuts, these should use the Shortcut key (i. e. Command on Mac, Control on other systems).
		Nodes.addInputMap(this, consume(anyOf(
			keyPressed(new KeyCharacterCombination("c", SHORTCUT_DOWN)),
			keyPressed(KeyCode.COPY),
			keyPressed(KeyCode.INSERT, SHORTCUT_DOWN)
		), e -> this.copy()));
		Nodes.addInputMap(this, consume(anyOf(
			keyPressed(new KeyCharacterCombination("v", SHORTCUT_DOWN)),
			keyPressed(KeyCode.PASTE),
			keyPressed(KeyCode.INSERT, SHIFT_DOWN)
		), e -> this.paste()));

		// Shell/Emacs-style shortcuts, these should always use Control as the modifier, even on Mac (this is how it works in a normal terminal window).
		Nodes.addInputMap(this, consume(keyPressed(new KeyCharacterCombination("r", CONTROL_DOWN)), e -> this.reverseSearch()));
		Nodes.addInputMap(this, consume(keyPressed(new KeyCharacterCombination("a", CONTROL_DOWN)), e -> this.moveToInputStart()));
		Nodes.addInputMap(this, consume(keyPressed(new KeyCharacterCombination("e", CONTROL_DOWN)), e -> this.moveToInputEnd()));
		Nodes.addInputMap(this, consume(keyPressed(new KeyCharacterCombination("k", CONTROL_DOWN)), e -> this.reset()));

		Nodes.addInputMap(this, consume(keyPressed(KeyCode.ESCAPE), e -> this.deactivateSearch()));
		Nodes.addInputMap(this, consume(keyPressed(KeyCode.UP), e -> this.handleUp()));
		Nodes.addInputMap(this, consume(keyPressed(KeyCode.DOWN), e -> this.handleDown()));
		Nodes.addInputMap(this, consume(keyPressed(KeyCode.LEFT, KeyCombination.SHIFT_ANY), this::handleLeft));
		Nodes.addInputMap(this, consume(keyPressed(KeyCode.RIGHT, KeyCombination.SHIFT_ANY), this::handleRight));
		Nodes.addInputMap(this, consume(keyPressed(KeyCode.DELETE), e -> this.handleDelete()));
		Nodes.addInputMap(this, consume(keyPressed(KeyCode.BACK_SPACE), e -> handleBackspace()));
		Nodes.addInputMap(this, consume(keyPressed(KeyCode.ENTER), e -> this.handleEnter()));
	}

	private void setDragDrop() {
		this.setOnDragOver(e -> {
			Dragboard dragboard = e.getDragboard();
			if (dragboard.hasFiles()) {
				e.acceptTransferModes(TransferMode.COPY);
			} else {
				e.consume();
			}
		});

		this.setOnDragDropped(e -> {
			Dragboard dragboard = e.getDragboard();
			boolean success = false;
			if (dragboard.hasFiles()) {
				success = true;
				onEnterText(
					dragboard.getFiles().stream()
						.map(File::getAbsolutePath)
						.collect(Collectors.joining(" "))
				);
			}
			e.setDropCompleted(success);
			e.consume();
		});
	}

	@Override
	protected void handleInputMethodEvent(InputMethodEvent event) {
		if (!event.getComposed().isEmpty()) {
			LOGGER.warn("ignoring InputMethodEvent composed elements {}", event.getComposed());
		}

		this.onEnterText(event.getCommitted());
	}

	@Override
	public void copy() {
		IndexRange selection = getSelection();
		if (selection.getLength() > 0) {
			ClipboardContent content = new ClipboardContent();
			content.putString(getSelectedText());
			Clipboard.getSystemClipboard().setContent(content);
		}
	}

	@Override
	public void paste() {
		Clipboard clipboard = Clipboard.getSystemClipboard();
		if (clipboard.hasString()) {
			String text = clipboard.getString();
			if (text != null) {
				onEnterText(text);
			}
		}
	}

	public void reverseSearch() {
		if (this.historyAndSearchHandler.isSearchActive()) {
			this.historyAndSearchHandler.searchNext();
		} else {
			activateSearch();
		}

		this.moveToInputEnd();
	}

	protected void onEnterText(String text) {
		text = text == null ? "" : text.replace("\r\n", "\n").replace('\r', '\n');
		int idx = 0;
		int eol;
		while (true) {
			eol = text.indexOf('\n', idx);
			if (eol < 0) {
				if (idx == 0 || idx < text.length()) {
					this.onEnterSingleLineText(text.substring(idx));
				}

				break;
			} else {
				this.onEnterSingleLineText(text.substring(idx, eol));
				this.handleEnter();
				idx = eol + 1;
			}
		}
	}

	protected void onEnterSingleLineText(String text) {
		this.moveCaretToInputEndIfRequired();

		if (text.isEmpty()) {
			return;
		}
		assert text.indexOf('\n') < 0 && text.indexOf('\r') < 0;

		String input = this.input.get();
		int anchorPosition = this.getAnchorPositionInInput().orElse(this.getAnchor() <= this.getCaretPosition() ? 0 : input.length());
		int inputPosition = this.getPositionInInput().orElseThrow(() -> new AssertionError("caret not in input"));
		int start = Math.min(anchorPosition, inputPosition);
		int end = Math.max(anchorPosition, inputPosition);

		String prefix = input.substring(0, start);
		String suffix = input.substring(end);
		this.input.set(prefix + text + suffix);
		this.moveCaretToPosInInput(start + text.length());
	}

	public void moveCaretToPosInInput(int pos) {
		this.moveCaretToPosInInput(pos, SelectionPolicy.CLEAR);
	}

	public void moveCaretToPosInInput(int pos, SelectionPolicy selectionPolicy) {
		assert !this.getParagraphs().isEmpty();
		this.moveTo(this.getParagraphs().size() - 1, this.inputStart.get() + pos, selectionPolicy);
		this.requestFollowCaret();
	}

	public void moveToInputStart() {
		this.moveCaretToPosInInput(0);
	}

	public void moveToInputEnd() {
		this.moveCaretToPosInInput(this.input.get().length());
	}

	public void moveCaretToInputEndIfRequired() {
		if (this.getPositionInInput().isEmpty()) {
			this.moveToInputEnd();
		}
	}

	protected void activateSearch() {
		this.historyAndSearchHandler.setSearchActive(true);
	}

	protected void deactivateSearch() {
		this.historyAndSearchHandler.setSearchActive(false);
	}

	public void reset() {
		this.lastMessage = null;
		this.historyAndSearchHandler.setSearchActive(false);
		this.lineContinuation.set(false);
		this.clear();
		this.input.set("");
		this.update();
		this.moveCaretToInputEndIfRequired();
	}

	/**
	 * Insert text above the current prompt.
	 *
	 * @param text text to insert
	 */
	public void addParagraph(String text) {
		this.addParagraph(text, Set.of(), true);
	}

	/**
	 * Insert text above the current prompt.
	 *
	 * @param text  text to insert
	 * @param style text style
	 */
	public void addParagraph(String text, Collection<String> style, boolean allowDuplicates) {
		if (!allowDuplicates) {
			CachedMessage newMessage = new CachedMessage(text, style);
			if (newMessage.equals(this.lastMessage)) {
				return;
			}

			this.lastMessage = newMessage;
		} else {
			this.lastMessage = null;
		}

		int lastParagraph = this.getParagraphs().size() - 1;
		assert lastParagraph >= 0;
		int pos = this.getAbsolutePosition(lastParagraph, 0);
		this.insert(pos, text + "\n", style);
		this.requestFollowCaret();
	}

	protected void handleEnter() {
		this.moveCaretToInputEndIfRequired();

		this.deactivateSearch();
		String command = this.input.get();
		String inputWithPrompt = this.inputWithPrompt.get();
		this.input.set("");
		this.addParagraph(inputWithPrompt);

		boolean activateLineContinuation = command != null && command.endsWith("\\") && !command.endsWith("\\\\");
		this.lineContinuation.set(activateLineContinuation);

		// TODO: maybe cut off trailing backslash?
		historyAndSearchHandler.enter(command);

		if (command != null && !command.isEmpty()) {
			if (!commandBuffer.isEmpty()) {
				commandBuffer.append('\n');
			}

			commandBuffer.append(command);
		}

		if (!activateLineContinuation) {
			String realCommand = commandBuffer.toString();
			commandBuffer.setLength(0);

			if (!realCommand.isEmpty()) {
				ConsoleExecResult result = interpreter.exec(realCommand);
				if (result.getResultType() == ConsoleExecResultType.CLEAR) {
					reset();
					return;
				}

				this.addParagraph(result.toString(), result.getResultType() == ConsoleExecResultType.ERROR ? Set.of("error", "output") : Set.of("output"), true);
			}
		}

		this.moveCaretToInputEndIfRequired();
	}

	private void handleDown() {
		deactivateSearch();
		historyAndSearchHandler.down();
	}

	private void handleUp() {
		deactivateSearch();
		historyAndSearchHandler.up();
	}

	private void handleLeft(KeyEvent e) {
		if (this.isSearching()) {
			this.historyAndSearchHandler.setSearchActive(false);
			return;
		}

		OptionalInt inputPos = this.getPositionInInput();
		if (inputPos.isPresent()) {
			boolean shift = e.isShiftDown();
			int oldPos = inputPos.getAsInt();
			String input = this.input.get();
			if (oldPos > 0) {
				int newPos = input.offsetByCodePoints(oldPos, -1);
				this.moveCaretToPosInInput(newPos, shift ? SelectionPolicy.ADJUST : SelectionPolicy.CLEAR);
			} else if (!shift) {
				this.getCaretSelectionBind().deselect();
			}
		} else {
			this.moveToInputEnd();
		}
	}

	private void handleRight(KeyEvent e) {
		if (this.isSearching()) {
			this.historyAndSearchHandler.setSearchActive(false);
			return;
		}

		OptionalInt inputPos = this.getPositionInInput();
		if (inputPos.isPresent()) {
			boolean shift = e.isShiftDown();
			int oldPos = inputPos.getAsInt();
			String input = this.input.get();
			if (oldPos < input.length()) {
				int newPos = input.offsetByCodePoints(oldPos, 1);
				this.moveCaretToPosInInput(newPos, shift ? SelectionPolicy.ADJUST : SelectionPolicy.CLEAR);
			} else if (!shift) {
				this.getCaretSelectionBind().deselect();
			}
		} else {
			this.moveToInputStart();
		}
	}

	private boolean deleteSelectedText() {
		String input = this.input.get();
		OptionalInt anchorPosOpt = this.getAnchorPositionInInput();
		OptionalInt caretPosOpt = this.getPositionInInput();
		if (anchorPosOpt.isPresent() || caretPosOpt.isPresent()) {
			boolean anchorBeforeCaret = this.getAnchor() <= this.getCaretPosition();
			int anchorPos = anchorPosOpt.orElse(anchorBeforeCaret ? 0 : input.length());
			int caretPos = caretPosOpt.orElse(anchorBeforeCaret ? input.length() : 0);
			int start = Math.min(anchorPos, caretPos);
			int end = Math.max(anchorPos, caretPos);
			if (start == end) {
				return false;
			}

			this.input.set(input.substring(0, start) + input.substring(end));
			this.moveCaretToPosInInput(start);
		}

		return true;
	}

	private void handleBackspace() {
		if (deleteSelectedText()) {
			return;
		}

		this.getPositionInInput().ifPresent(end -> {
			if (end > 0) {
				String input = this.input.get();
				int start = input.offsetByCodePoints(end, -1);
				this.input.set(input.substring(0, start) + input.substring(end));
				this.moveCaretToPosInInput(start);
			}
		});
	}

	private void handleDelete() {
		if (deleteSelectedText()) {
			return;
		}

		this.getPositionInInput().ifPresent(start -> {
			if (start < this.input.get().length()) {
				String input = this.input.get();
				int end = input.offsetByCodePoints(start, 1);
				this.input.set(input.substring(0, start) + input.substring(end));
				this.moveCaretToPosInInput(start);
			}
		});
	}

	protected OptionalInt getPositionInInput() {
		return this.getPositionInInput(this.getCurrentParagraph(), this.getCaretColumn());
	}

	protected OptionalInt getAnchorPositionInInput() {
		return this.getPositionInInput(this.getCaretSelectionBind().getAnchorParIndex(), this.getCaretSelectionBind().getAnchorColPosition());
	}

	protected OptionalInt getPositionInInput(int par, int col) {
		int lastParagraph = this.getParagraphs().size() - 1;
		assert lastParagraph >= 0;
		if (lastParagraph != par) {
			return OptionalInt.empty();
		}

		int inputPosition = col - this.inputStart.get();
		if (inputPosition < 0 || inputPosition > this.input.get().length()) {
			return OptionalInt.empty();
		}

		return OptionalInt.of(inputPosition);
	}

	protected void replace(int start, int end, String text) {
		if (start > end) {
			throw new IllegalArgumentException();
		}

		String prefix = this.getInput().substring(0, Math.max(0, Math.min(this.getInput().length(), start)));
		String suffix = this.getInput().substring(Math.max(0, Math.min(this.getInput().length(), end)));
		this.setInput(prefix + text + suffix);
		this.moveCaretToPosInInput(prefix.length() + text.length());
	}

	public boolean isSearching() {
		return this.historyAndSearchHandler.isSearchActive();
	}

	public ObservableList<String> getHistory() {
		return historyAndSearchHandler.getHistory();
	}

	public void setHistory(List<String> history) {
		historyAndSearchHandler.setHistory(history);
	}

	public StringProperty promptProperty() {
		return this.prompt;
	}

	public String getPrompt() {
		return this.promptProperty().get();
	}

	public void setPrompt(final String prompt) {
		this.promptProperty().set(prompt);
	}

	public ReadOnlyStringProperty inputProperty() {
		return input;
	}

	public String getInput() {
		return this.inputProperty().get();
	}

	public void setInput(String input) {
		this.input.set(input);
		this.moveToInputEnd();
	}

	public int getInputStart() {
		return this.inputStart.get();
	}

	public int getInputEnd() {
		return this.inputEnd.get();
	}

	public ObservableValue<Optional<Point2D>> caretPosProperty() {
		return caretPos;
	}

	public ObservableValue<Optional<String>> textBeforeCaretProperty() {
		return textBeforeCaret;
	}

	public Executable getInterpreter() {
		return this.interpreter;
	}

	protected abstract class AbstractParentWithEditableText<T extends CodeCompletionItem> implements ParentWithEditableText<T> {

		@Override
		public Window getWindow() {
			return Console.this.getScene().getWindow();
		}

		@Override
		public ObservableValue<Optional<Point2D>> getCaretPosition() {
			return Console.this.caretPosProperty();
		}

		@Override
		public ObservableValue<Optional<String>> getTextBeforeCaret() {
			return Console.this.textBeforeCaretProperty();
		}
	}
}
