package de.prob2.ui.consoles;

import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StringHelper;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.IntegerExpression;
import javafx.beans.binding.StringBinding;
import javafx.beans.binding.StringExpression;
import javafx.beans.property.*;
import javafx.collections.ObservableList;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.IndexRange;
import javafx.scene.control.MenuItem;
import javafx.scene.input.*;
import org.controlsfx.tools.Platform;
import org.fxmisc.richtext.StyleClassedTextArea;
import org.fxmisc.wellbehaved.event.Nodes;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static javafx.scene.input.KeyCombination.*;
import static org.fxmisc.wellbehaved.event.EventPattern.*;
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
    private final StringExpression inputWithPrompt;
    private final IntegerExpression inputStart;
    private final IntegerExpression inputEnd;

    private final ConsoleHistoryHandler historyHandler;
    private final ConsoleSearchHandler searchHandler;

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

        this.historyHandler = new ConsoleHistoryHandler(this);
        this.searchHandler = new ConsoleSearchHandler(this);

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
            if (searchHandler.isActive()) {
                String result = searchHandler.currentSearchResultProperty().get();
                if (result == null) {
                    return translatedFailedSearchPrompt.get().length() + 1;
                } else {
                    return translatedSuccessfulSearchPrompt.get().length() + 1;
                }
            } else {
                if (lineContinuation.get()) {
                    return lineContinuationPrompt.get().length();
                } else {
                    return translatedPrompt.get().length();
                }
            }
        }, this.lineContinuation, translatedPrompt, translatedLineContinuationPrompt, translatedSuccessfulSearchPrompt, translatedFailedSearchPrompt, this.input, this.searchHandler.searchActiveProperty(), this.searchHandler.currentSearchResultProperty());
        this.inputEnd = Bindings.createIntegerBinding(() -> this.inputStart.get() + this.input.get().length(), this.inputStart, this.input);
        this.inputWithPrompt = Bindings.createStringBinding(() -> {
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
        }, this.lineContinuation, translatedPrompt, translatedLineContinuationPrompt, translatedSuccessfulSearchPrompt, translatedFailedSearchPrompt, this.input, this.searchHandler.searchActiveProperty(), this.searchHandler.currentSearchResultProperty());
        this.inputWithPrompt.addListener((o, from, to) -> this.update(from, to));

        this.reset();
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

    private void update() {
        this.update(null, this.inputWithPrompt.get());
    }

    private void update(String from, String to) {
        int caretParagraph = this.getCurrentParagraph();
        int caretColumn = this.getCaretColumn();

        boolean empty = this.getLength() == 0;
        if (empty) {
            this.insertText(0, i18n.translate(this.header.get()) + "\n");
        }

        assert this.getParagraphs().size() >= 2; // always at least header + prompt
        int lastParagraph = this.getParagraphs().size() - 1;
        int paragraphLength = this.getParagraphLength(lastParagraph);
        this.replaceText(lastParagraph, 0, lastParagraph, paragraphLength, to);

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
        Nodes.addInputMap(this, consume(mouseClicked(MouseButton.PRIMARY), e -> this.onMouseClicked()));
        Nodes.addInputMap(this, consume(keyPressed()));
        Nodes.addInputMap(this, consume(keyReleased()));
        Nodes.addInputMap(this, consume(keyTyped()));
        Nodes.addInputMap(this, consume(keyTyped().onlyIf(Console::hasInsertableText), e -> this.onEnterText(e.getCharacter())));

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
        Nodes.addInputMap(this, consume(keyPressed(new KeyCharacterCombination("a", CONTROL_DOWN)), e -> this.moveToStart()));
        Nodes.addInputMap(this, consume(keyPressed(new KeyCharacterCombination("e", CONTROL_DOWN)), e -> this.moveToEnd()));
        Nodes.addInputMap(this, consume(keyPressed(new KeyCharacterCombination("k", CONTROL_DOWN)), e -> this.reset()));

        Nodes.addInputMap(this, consume(keyPressed(KeyCode.UP), e -> this.handleUp()));
        Nodes.addInputMap(this, consume(keyPressed(KeyCode.DOWN), e -> this.handleDown()));
        Nodes.addInputMap(this, consume(keyPressed(KeyCode.LEFT), e -> this.handleLeft()));
        Nodes.addInputMap(this, consume(keyPressed(KeyCode.RIGHT), e -> this.handleRight()));
        Nodes.addInputMap(this, consume(keyPressed(KeyCode.DELETE), e -> this.handleDelete()));
        Nodes.addInputMap(this, consume(keyPressed(KeyCode.BACK_SPACE), e -> handleBackspace()));
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

    private void onMouseClicked() {
        // System.out.printf("mouseClicked: %d %d%n", this.getLength(), this.getCaretPosition());
        int caretParagraph = this.getCurrentParagraph();
        int caretColumn = this.getCaretColumn();

		/*if (this.getLength() - 1 - this.getCaretPosition() < charCounterInLine) {
			currentPosInLine = charCounterInLine - (this.getLength() - this.getCaretPosition());
		}*/
    }

    public void reverseSearch() {
        if (searchHandler.isActive()) {
            searchHandler.searchNext();
        } else {
            activateSearch();
        }
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
        moveCaretToInputIfRequired();

        if (text.isEmpty()) {
            return;
        }
        assert text.indexOf('\n') < 0 && text.indexOf('\r') < 0;

        int caretColumn = this.getCaretColumn();
        int lastParagraph = this.getParagraphs().size() - 1;
        int inputPosition = caretColumn - this.inputStart.get();
        assert lastParagraph >= 0 && inputPosition >= 0 && inputPosition <= this.input.get().length();

        String prefix = this.input.get().substring(0, inputPosition);
        String suffix = this.input.get().substring(inputPosition);
        this.input.set(prefix + text + suffix);
        this.moveTo(lastParagraph, caretColumn + text.length());
    }

    private void moveToStart() {
        assert this.getParagraphs().size() >= 2;
        this.moveTo(this.getParagraphs().size() - 1, this.inputStart.get());
    }

    private void moveToEnd() {
        assert this.getParagraphs().size() >= 2;
        this.moveTo(this.getParagraphs().size() - 1, this.inputEnd.get());
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
        // TODO: fix reverse search
		/*if (searchHandler.isActive()) {
			String searchResult = searchHandler.getCurrentSearchResult();
			this.deleteText(getLineNumber(), 0, getLineNumber(), this.getParagraphLength(getLineNumber()));
			this.appendText((instructionLengthInLine > 1 ? EMPTY_PROMPT : prompt.get()) + " " + searchResult);
			this.moveTo(this.getLength());
			charCounterInLine = searchResult.length();
			currentPosInLine = charCounterInLine;
			searchHandler.deactivateSearch();
		}*/
    }

    public void reset() {
        this.searchHandler.deactivateSearch();
        this.clear();
        this.input.set("");
        this.update();
    }

    protected void handleEnter() {
        moveCaretToInputIfRequired();
        String command = input.get();

        // TODO: execute input/search result
        // TODO: deactivate search
        // TODO: append executed command and output to text area
        // TODO: mind line continuations!
        System.out.println("'" + command + "'");

        int lastParagraph = this.getParagraphs().size() - 1;
        assert lastParagraph >= 0;
        this.insertText(lastParagraph, 0, this.inputWithPrompt.get() + "\n");
        input.set("");

		/*charCounterInLine = 0;
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
		goToLastPos();*/
    }

    private void handleDown() {
        deactivateSearch();
        historyHandler.down();
    }

    private void handleUp() {
        deactivateSearch();
        historyHandler.up();
    }

    private void handleLeft() {
        // TODO: move caret left
		/*deactivateSearch();
		if (currentPosInLine > 0 && this.getLength() - this.getCaretPosition() <= charCounterInLine) {
			currentPosInLine--;
			this.moveTo(this.getCaretPosition() - 1);
		} else if (currentPosInLine == 0) {
			super.deselect();
		}*/
    }

    private void handleRight() {
        // TODO: move caret right
		/*deactivateSearch();
		if (currentPosInLine < charCounterInLine && this.getLength() - this.getCaretPosition() <= charCounterInLine) {
			currentPosInLine++;
			this.moveTo(this.getCaretPosition() + 1);
		}*/
    }

    private void handleDeletion(KeyEvent e) {
		/*int maxPosInLine = charCounterInLine;
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
		}*/
    }

    private void handleBackspace() {
        // TODO: check if cursor is in input and then delete char before
        // TODO: mind multi-char codepoints!
		/*if (currentPosInLine > 0) {
			currentPosInLine = Math.max(currentPosInLine - 1, 0);
			charCounterInLine = Math.max(charCounterInLine - 1, 0);
			this.deletePreviousChar();
		}*/
    }

    private void handleDelete() {
        // TODO: check if cursor is in input and then delete char before
        // TODO: mind multi-char codepoints!
		/*if (currentPosInLine < charCounterInLine) {
			charCounterInLine = Math.max(charCounterInLine - 1, 0);
			this.deleteNextChar();
		}*/
    }

    private void moveCaretToInputIfRequired() {
        int lastParagraph = this.getParagraphs().size() - 1;
        int caretParagraph = this.getCurrentParagraph();
        int caretColumn = this.getCaretColumn();
        if (caretParagraph != lastParagraph || caretColumn < this.inputStart.get() || caretColumn > this.inputEnd.get()) {
            this.moveTo(lastParagraph, this.inputEnd.get(), SelectionPolicy.CLEAR);
        }
    }

    public boolean isSearching() {
        return searchHandler.isActive();
    }

    public ObservableList<String> getHistory() {
        return historyHandler.getHistory();
    }

    public void setHistory(List<String> history) {
        historyHandler.setHistory(history);
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
        return this.input.get();
    }

    public void setInput(String input) {
        this.input.set(input);
        this.moveToEnd();
    }
}
