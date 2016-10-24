package de.prob2.ui.consoles.groovy;

import java.io.File;

import de.prob2.ui.consoles.Console;
import de.prob2.ui.consoles.ConsoleInstruction;
import de.prob2.ui.consoles.groovy.codecompletion.CodeCompletionEvent;
import de.prob2.ui.consoles.groovy.codecompletion.CodeCompletionTriggerAction;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;


public class GroovyConsole extends Console {
	
	private GroovyInterpreter interpreter;
	
	public GroovyConsole() {
		super();
		this.appendText("Prob 2.0 Groovy Console \n >");
	}
	
	public void reset() {
		this.setText("Prob 2.0 Groovy Console");
	}
	
	public void setInterpreter(GroovyInterpreter interpreter) {
		this.interpreter = interpreter;
	}
	
	@Override
	protected void handleInsertChar(KeyEvent e) {
		if(".".equals(e.getText())) {
			triggerCodeCompletion(CodeCompletionTriggerAction.POINT);
		}
		super.handleInsertChar(e);
	}
	
	@Override
	protected void setListeners() {
		super.setListeners();
		setCodeCompletionEvent();
		setDragDrop();
	}
	
	@Override
	protected void setKeyEvent() {
		super.setKeyEvent();
		this.addEventFilter(KeyEvent.ANY, e -> {
			if(e.isControlDown() && e.getCode() == KeyCode.SPACE) {
				triggerCodeCompletion(CodeCompletionTriggerAction.TRIGGER);
			}
		});
	}
	
	private void triggerCodeCompletion(CodeCompletionTriggerAction action) {
		if(getCaretPosition() > this.getText().lastIndexOf("\n") + 2) {
			int caretPosInLine = getCurrentLine().length() - (getLength() - getCaretPosition());
			interpreter.triggerCodeCompletion(this, getCurrentLine().substring(0, caretPosInLine), action);
		}
	}
	
	private void setCodeCompletionEvent() {
		this.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> interpreter.triggerCloseCodeCompletion());
		this.addEventHandler(CodeCompletionEvent.CODECOMPLETION, this::handleCodeCompletionEvent);
	}
	
	
	private void setDragDrop() {
		this.setOnDragOver(e-> {
            Dragboard dragbord = e.getDragboard();
            if (dragbord.hasFiles()) {
                e.acceptTransferModes(TransferMode.COPY);
            } else {
                e.consume();
            }
		});
		
		this.setOnDragDropped(e-> {
            Dragboard dragbord = e.getDragboard();
            boolean success = false;
            int lastPosOfEnter = this.getText().lastIndexOf("\n");
            if (dragbord.hasFiles() && this.getCaretPosition() >= lastPosOfEnter + 3) {
                success = true;
                String path = null;
                for (File file : dragbord.getFiles()) {
                    path = file.getAbsolutePath();
                    String newText = new StringBuilder(this.getText()).insert(this.getCaretPosition(), path).toString();
                    int caretPosition = this.getCaretPosition();
                    this.setText(newText);
                    charCounterInLine += path.length();
                    currentPosInLine += path.length();
                    this.positionCaret(caretPosition + path.length());
                }
            }
            e.setDropCompleted(success);
            e.consume();
		});
	}
	
	private void handleCodeCompletionEvent(CodeCompletionEvent e) {
		if(e.getCode() == KeyCode.ENTER || e.getEvent() instanceof MouseEvent || ";".equals(((KeyEvent)e.getEvent()).getText())) {
			handleChooseSuggestion(e);
		} else if(((CodeCompletionEvent)e).getCode() == KeyCode.SPACE) {
			//handle Space in Code Completion
			handleInsertChar((KeyEvent)e.getEvent());
			e.consume();
		}
	}
	
	private void handleChooseSuggestion(CodeCompletionEvent e) {
		String choice = ((CodeCompletionEvent) e).getChoice();
		String suggestion = ((CodeCompletionEvent) e).getCurrentSuggestion();
		String newText = this.getText().substring(0, this.getCaretPosition() - suggestion.length());
		newText = new StringBuilder(newText).append(choice).toString();
		newText = new StringBuilder(newText).append(this.getText().substring(this.getCaretPosition())).toString();
		int diff = newText.length() - this.getText().length();
		int caret = this.getCaretPosition();
		this.setText(newText);
		currentPosInLine += diff;
		charCounterInLine += diff;
		this.positionCaret(caret + diff);
	}
	
	@Override
	protected void handleEnter(KeyEvent e) {
		super.handleEnterAbstract(e);
		ConsoleInstruction instruction = instructions.get(posInList);
		if(getCurrentLine().isEmpty()) {
			this.appendText("\n null");
		} else {
			if("clear".equals(interpreter.exec(instruction).getConsoleOutput())) {
				reset();
			} else {
				this.appendText("\n" + interpreter.exec(instruction));
			}
		}
		this.appendText("\n >");
	}
	
		
	public void closeObjectStage() {
		interpreter.closeObjectStage();
	}	

}
