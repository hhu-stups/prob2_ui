package de.prob2.ui.consoles;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StringHelper;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.Clipboard;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCharacterCombination;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.TransferMode;

import org.controlsfx.tools.Platform;
import org.fxmisc.richtext.StyleClassedTextArea;
import org.fxmisc.wellbehaved.event.Nodes;

import static javafx.scene.input.KeyCombination.CONTROL_DOWN;
import static javafx.scene.input.KeyCombination.SHIFT_DOWN;
import static javafx.scene.input.KeyCombination.SHORTCUT_DOWN;
import static org.fxmisc.wellbehaved.event.EventPattern.anyOf;
import static org.fxmisc.wellbehaved.event.EventPattern.keyPressed;
import static org.fxmisc.wellbehaved.event.EventPattern.keyReleased;
import static org.fxmisc.wellbehaved.event.EventPattern.keyTyped;
import static org.fxmisc.wellbehaved.event.EventPattern.mouseClicked;
import static org.fxmisc.wellbehaved.event.InputMap.consume;

public abstract class Console extends StyleClassedTextArea {

	private final I18n i18n;
	private final Executable interpreter;

	private final BooleanProperty lineContinuation;
	private final StringProperty header;
	private final StringProperty prompt;
	private final StringProperty lineContinuationPrompt;
	private final StringProperty successfulSearchPrompt;
	private final StringProperty failedSearchPrompt;
	private final StringProperty input;
	private final StringProperty inputWithPrompt;

	private final ObservableList<ConsoleInstruction> history;
	private final ConsoleSearchHandler searchHandler;

	protected int charCounterInLine = 0;
	protected int currentPosInLine = 0;
	protected int posInList = -1;
	protected int instructionLengthInLine = 1;

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
		this.inputWithPrompt = new SimpleStringProperty(this, "inputWithPrompt", null);

		this.history = FXCollections.observableArrayList();
		this.searchHandler = new ConsoleSearchHandler(i18n, this);

		this.requestFollowCaret();
		initializeContextMenu();
		setEvents();
		setDragDrop();
		this.reset();
		this.setWrapText(true);
		this.getStyleClass().add("console");

		StringBinding translatedPrompt = i18n.translateBinding(this.prompt);
		StringBinding translatedLineContinuationPrompt = i18n.translateBinding(this.lineContinuationPrompt);
		StringBinding translatedSuccessfulSearchPrompt = i18n.translateBinding(this.successfulSearchPrompt);
		StringBinding translatedFailedSearchPrompt = i18n.translateBinding(this.failedSearchPrompt);
		this.inputWithPrompt.bind(Bindings.createStringBinding(() -> {
			if (searchHandler.isActive()) {
				String result = searchHandler.currentSearchResultProperty().get();
				if (result == null) {
					return translatedFailedSearchPrompt.get() + "`" + input.get() + "': ";
				} else {
					return translatedSuccessfulSearchPrompt.get() + "`" + input.get() + "': " + result;
				}
			} else {
				if (lineContinuation.get()) {
					return lineContinuationPrompt.get() + input.get();
				} else {
					return translatedPrompt.get() + input.get();
				}
			}
		}, this.lineContinuation, translatedPrompt, translatedLineContinuationPrompt, translatedSuccessfulSearchPrompt, translatedFailedSearchPrompt, this.input, this.searchHandler.searchActiveProperty(), this.searchHandler.currentSearchResultProperty()));
		this.inputWithPrompt.addListener((o, from, to) -> this.update(from, to));

		this.promptProperty().addListener((o, from, to) -> {
			// If the cursor is in the input, remember its position relative to the end of the prompt, and place it there again after the prompt is updated.
			final int caretPositionInInput;
			if (this.getCaretPosition() >= this.getLineStart()) {
				caretPositionInInput = Math.max(this.getCaretColumn() - from.length(), 0);
			} else {
				caretPositionInInput = -1;
			}
			this.replace(this.getLineNumber(), 0, this.getLineNumber(), from.length(), to, this.getStyleAtPosition(this.getLineNumber(), 0));
			if (caretPositionInInput != -1) {
				this.moveTo(this.getLineNumber(), caretPositionInInput + to.length());
			}
		});

		this.update(null, inputWithPrompt.get());
	}

	private void update(String from, String to) {
		if (this.getLength() == 0) {
			super.insertText(0, i18n.translate(this.header.get()) + "\n");
		}

		int lastParagraph = this.getParagraphs().size() - 1;
		this.replaceText(lastParagraph, 0, lastParagraph, 1000, to);
	}

	private static boolean hasInsertableText(KeyEvent event) {
		if (event.getCharacter().isEmpty() || !StringHelper.containsNoControlCharacters(event.getCharacter())) {
			return false;
		}

		if (Platform.getCurrent() == Platform.WINDOWS) {
			if (event.isControlDown() && event.isAltDown() && !event.getCharacter().isEmpty() && event.getCharacter().charAt(0) != '\0') {
				return true;
			} else {
				return !event.isControlDown() && !event.isAltDown() && !event.isMetaDown();
			}
		} else {
			return !event.isControlDown() && !event.isMetaDown();
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
		Nodes.addInputMap(this, consume(mouseClicked(MouseButton.PRIMARY), e -> this.onMouseClicked()));
		Nodes.addInputMap(this, consume(keyPressed()));
		Nodes.addInputMap(this, consume(keyReleased()));
		Nodes.addInputMap(this, consume(keyTyped()));
		Nodes.addInputMap(this, consume(keyTyped().onlyIf(Console::hasInsertableText), e -> this.onKeyTyped(e.getCharacter())));

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
		Nodes.addInputMap(this, consume(keyPressed(new KeyCharacterCombination("r", CONTROL_DOWN)), e -> this.controlR()));
		Nodes.addInputMap(this, consume(keyPressed(new KeyCharacterCombination("a", CONTROL_DOWN)), e -> this.controlA()));
		Nodes.addInputMap(this, consume(keyPressed(new KeyCharacterCombination("e", CONTROL_DOWN)), e -> this.controlE()));
		Nodes.addInputMap(this, consume(keyPressed(new KeyCharacterCombination("k", CONTROL_DOWN)), e -> this.reset()));

		Nodes.addInputMap(this, consume(keyPressed(KeyCode.UP), e -> this.handleUp()));
		Nodes.addInputMap(this, consume(keyPressed(KeyCode.DOWN), e -> this.handleDown()));
		Nodes.addInputMap(this, consume(keyPressed(KeyCode.LEFT), e -> this.handleLeft()));
		Nodes.addInputMap(this, consume(keyPressed(KeyCode.RIGHT), e -> this.handleRight()));
		Nodes.addInputMap(this, consume(keyPressed(KeyCode.DELETE), this::handleDeletion));
		Nodes.addInputMap(this, consume(keyPressed(KeyCode.BACK_SPACE), this::handleDeletion));
		Nodes.addInputMap(this, consume(keyPressed(KeyCode.ENTER), e -> this.handleEnter()));

		/*Nodes.addInputMap(this, InputMap.process(KeyEvent.KEY_PRESSED, e -> {
			System.out.printf("[%s, text=%s, char=%s, code=%s%s%s%s%s%s]%n", e.getEventType(), escapeNonAscii(e.getText()), escapeNonAscii(e.getCharacter()), e.getCode(), e.isShiftDown() ? " SHIFT" : "", e.isControlDown() ? " CTRL" : "", e.isAltDown() ? " ALT" : "", e.isMetaDown() ? " META" : "", e.isShortcutDown() ? " SHRTCT" : "");
			return InputHandler.Result.PROCEED;
		}));
		Nodes.addInputMap(this, InputMap.process(KeyEvent.KEY_TYPED, e -> {
			System.out.printf("[%s, text=%s, char=%s, code=%s%s%s%s%s%s]%n", e.getEventType(), escapeNonAscii(e.getText()), escapeNonAscii(e.getCharacter()), e.getCode(), e.isShiftDown() ? " SHIFT" : "", e.isControlDown() ? " CTRL" : "", e.isAltDown() ? " ALT" : "", e.isMetaDown() ? " META" : "", e.isShortcutDown() ? " SHRTCT" : "");
			return InputHandler.Result.PROCEED;
		}));*/
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
			if (dragboard.hasFiles() && this.getCaretPosition() >= this.getInputStart()) {
				success = true;
				for (File file : dragboard.getFiles()) {
					String path = file.getAbsolutePath();
					int caretPosition = this.getCaretPosition();
					this.insertText(this.getCaretPosition(), path);
					charCounterInLine += path.length();
					currentPosInLine += path.length();
					this.moveTo(caretPosition + path.length());
				}
			}
			e.setDropCompleted(success);
			e.consume();
		});
	}

	@Override
	public void paste() {
		if (searchHandler.isActive()) {
			return;
		}
		if (this.getLength() - 1 - this.getCaretPosition() >= charCounterInLine) {
			goToLastPos();
		}

		final int oldLength = this.getLength();
		String[] pastedLines = Clipboard.getSystemClipboard().getString().split("\n");
		for (int i = 0; i < pastedLines.length - 1; i++) {
			this.appendText(pastedLines[i] + "\\");
			handleEnter();
		}
		this.appendText(pastedLines[pastedLines.length - 1]);
		final int diff = this.getLength() - oldLength;

		charCounterInLine += diff;
		currentPosInLine += diff;
	}

	private void onMouseClicked() {
		// System.out.printf("mouseClicked: %d %d %d %d%n", this.getLength(), this.getCaretPosition(), currentPosInLine, charCounterInLine);
		if (this.getLength() - 1 - this.getCaretPosition() < charCounterInLine) {
			currentPosInLine = charCounterInLine - (this.getLength() - this.getCaretPosition());
		}
	}

	public void controlR() {
		if (searchHandler.isActive()) {
			searchHandler.searchNext();
		} else {
			activateSearch();
		}
	}

	protected void onKeyTyped(String character) {
		if (this.getLength() - this.getCaretPosition() > charCounterInLine) {
			goToLastPos();
		}

		// System.out.println("keyTyped: '" + escapeNonAscii(character) + "' line before='" + getLine() + "'");
		this.insertText(this.getCaretPosition(), character);
		// System.out.println("  line after='" + getLine() + "'");
		charCounterInLine += character.length();
		currentPosInLine += character.length();
		posInList = history.size() - 1;
		searchHandler.update();
	}

	private void controlA() {
		if (!searchHandler.isActive()) {
			this.moveTo(this.getCaretPosition() - currentPosInLine);
			currentPosInLine = 0;
		}
	}

	private void controlE() {
		if (!searchHandler.isActive()) {
			this.moveTo(this.getLength());
			currentPosInLine = charCounterInLine;
		}
	}

	protected void activateSearch() {
		// TODO: fix reverse search
		/*final String input = this.getInput();
		this.deleteText(getLineNumber(), 0, getLineNumber(), this.getParagraphLength(getLineNumber()));
		this.appendText(i18n.translate("consoles.prompt.backwardSearch", "", input));
		this.moveTo(getLineNumber(), this.getLine().lastIndexOf('\''));
		currentPosInLine = 0;
		charCounterInLine = 0;
		searchHandler.activateSearch();*/
	}

	protected void deactivateSearch() {
		if (searchHandler.isActive()) {
			String searchResult = searchHandler.getCurrentSearchResult();
			this.deleteText(getLineNumber(), 0, getLineNumber(), this.getParagraphLength(getLineNumber()));
			this.appendText((instructionLengthInLine > 1 ? EMPTY_PROMPT : prompt.get()) + " " + searchResult);
			this.moveTo(this.getLength());
			charCounterInLine = searchResult.length();
			currentPosInLine = charCounterInLine;
			searchHandler.deactivateSearch();
		}
	}

	private void goToLastPos() {
		this.moveTo(this.getLength());
		deselect();
		currentPosInLine = charCounterInLine;
		requestFollowCaret(); //This forces the text area to scroll to the bottom. Invoking scrollYToPixel does not have the expected effect
	}

	public void reset() {
		this.replaceText(header + '\n' + prompt.get() + ' ');
	}

	protected void handleEnter() {
		charCounterInLine = 0;
		currentPosInLine = 0;
		String currentLine;
		if (searchHandler.isActive()) {
			currentLine = searchHandler.getCurrentSearchResult();
		} else {
			currentLine = this.getInput();
		}
		boolean endsWithNewline = currentLine.endsWith("\\");
		if (endsWithNewline) {
			currentLine = currentLine.substring(0, currentLine.length() - 1);
		}
		if (!history.isEmpty() && history.get(history.size() - 1).getOption() != ConsoleInstructionOption.ENTER) {
			history.set(history.size() - 1, new ConsoleInstruction(currentLine, ConsoleInstructionOption.ENTER));
		} else {
			history.add(new ConsoleInstruction(currentLine, ConsoleInstructionOption.ENTER));
		}
		if (endsWithNewline) {
			instructionLengthInLine++;
			this.appendText("\n" + EMPTY_PROMPT + " ");
			return;
		}

		posInList = history.size() - 1;

		ConsoleInstruction instruction;
		if (instructionLengthInLine > 1) {
			StringBuilder actualInstructionBuilder = new StringBuilder();
			for (int i = 0; i < instructionLengthInLine; i++) {
				ConsoleInstruction listInstruction = history.get(history.size() - instructionLengthInLine + i);
				actualInstructionBuilder.append(listInstruction.getInstruction()).append('\n');
			}

			instruction = new ConsoleInstruction(actualInstructionBuilder.toString(), ConsoleInstructionOption.ENTER);
		} else {
			instruction = history.get(posInList);
		}

		ConsoleExecResult execResult = interpreter.exec(instruction);
		int from = this.getLength();
		if (execResult.getResultType() == ConsoleExecResultType.CLEAR) {
			reset();
			return;
		}
		this.appendText("\n" + execResult);
		if (execResult.getResultType() == ConsoleExecResultType.ERROR) {
			this.setStyle(from, from + execResult.toString().length() + 1, Collections.singletonList("error"));
		}
		instructionLengthInLine = 1;
		searchHandler.handleEnter();
		this.appendText('\n' + prompt.get() + ' ');
		this.setStyle(getLineNumber(), Collections.emptyList());
		goToLastPos();
	}

	private void handleDown() {
		deactivateSearch();
		if (history.isEmpty() || posInList == history.size() - 1) {
			return;
		}
		posInList = Math.min(posInList + 1, history.size() - 1);
		setTextAfterArrowKey();
	}

	private void handleUp() {
		deactivateSearch();
		if (history.isEmpty() || posInList == -1) {
			return;
		}
		if (posInList == history.size() - 1) {
			String lastinstruction = history.get(history.size() - 1).getInstruction();
			if (!lastinstruction.equals(this.getInput())) {
				if (history.get(posInList).getOption() == ConsoleInstructionOption.UP) {
					history.set(history.size() - 1, new ConsoleInstruction(this.getInput(), ConsoleInstructionOption.UP));
				} else {
					history.add(new ConsoleInstruction(this.getInput(), ConsoleInstructionOption.UP));
					setTextAfterArrowKey();
					return;
				}
			}
		}
		posInList = Math.max(posInList - 1, 0);
		setTextAfterArrowKey();
	}

	private void handleLeft() {
		deactivateSearch();
		if (currentPosInLine > 0 && this.getLength() - this.getCaretPosition() <= charCounterInLine) {
			currentPosInLine--;
			this.moveTo(this.getCaretPosition() - 1);
		} else if (currentPosInLine == 0) {
			super.deselect();
		}
	}

	private void handleRight() {
		deactivateSearch();
		if (currentPosInLine < charCounterInLine && this.getLength() - this.getCaretPosition() <= charCounterInLine) {
			currentPosInLine++;
			this.moveTo(this.getCaretPosition() + 1);
		}
	}

	private void setTextAfterArrowKey() {
		String currentLine = history.get(posInList).getInstruction();
		this.deleteText(this.getInputStart(), this.getLength());
		this.appendText(currentLine);
		charCounterInLine = currentLine.length();
		currentPosInLine = charCounterInLine;
	}

	private void handleDeletion(KeyEvent e) {
		int maxPosInLine = charCounterInLine;
		if (searchHandler.handleDeletion(e)) {
			return;
		}
		if (searchHandler.isActive()) {
			maxPosInLine = charCounterInLine + 2 + searchHandler.getCurrentSearchResult().length();
		}
		if (!this.getSelectedText().isEmpty() || this.getLength() - this.getCaretPosition() > maxPosInLine) {
			return;
		}
		if (e.getCode().equals(KeyCode.BACK_SPACE)) {
			handleBackspace();
		} else {
			handleDelete();
		}
	}

	private void handleBackspace() {
		if (currentPosInLine > 0) {
			currentPosInLine = Math.max(currentPosInLine - 1, 0);
			charCounterInLine = Math.max(charCounterInLine - 1, 0);
			this.deletePreviousChar();
		}
	}

	private void handleDelete() {
		if (currentPosInLine < charCounterInLine) {
			charCounterInLine = Math.max(charCounterInLine - 1, 0);
			this.deleteNextChar();
		}
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

	public int getLineNumber() {
		return this.getParagraphs().size() - 1;
	}

	public int getLineStart() {
		return this.getAbsolutePosition(getLineNumber(), 0);
	}

	public String getLine() {
		return this.getParagraph(this.getLineNumber()).getText();
	}

	private int getCurrentLinePromptLength() {
		final String currentLinePrompt = instructionLengthInLine > 1 ? EMPTY_PROMPT : this.getPrompt();
		// Add 1 to the length, for the space character after the prompt string.
		return currentLinePrompt.length() + 1;
	}

	public int getInputStart() {
		String line = this.getLine();
		return this.getLineStart() + Math.min(line.length(), this.getCurrentLinePromptLength());
	}

	public String getInput() {
		int length = this.getCurrentLinePromptLength();
		String line = this.getLine();
		return line.length() <= length ? "" : this.getLine().substring(length);
	}

	public List<String> saveInstructions() {
		return history.stream().map(ConsoleInstruction::getInstruction).collect(Collectors.toList());
	}

	public void loadInstructions(List<String> instructions) {
		this.history.clear();
		for (final String instruction : instructions) {
			this.history.add(new ConsoleInstruction(instruction, ConsoleInstructionOption.ENTER));
		}
		posInList = this.history.size();
	}

	public int getCurrentPosInLine() {
		return currentPosInLine;
	}

	public ObservableList<ConsoleInstruction> getHistory() {
		return history;
	}

	public boolean isSearching() {
		return searchHandler.isActive();
	}
}
