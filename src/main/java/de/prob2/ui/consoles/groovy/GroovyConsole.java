package de.prob2.ui.consoles.groovy;

import java.io.File;

import org.fxmisc.wellbehaved.event.EventPattern;
import org.fxmisc.wellbehaved.event.InputMap;
import org.fxmisc.wellbehaved.event.Nodes;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.consoles.Console;
import de.prob2.ui.consoles.ConsoleInstruction;
import de.prob2.ui.consoles.groovy.codecompletion.CodeCompletionEvent;
import de.prob2.ui.consoles.groovy.codecompletion.CodeCompletionTriggerAction;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;

@Singleton
public class GroovyConsole extends Console {
	
	private GroovyInterpreter interpreter;
		
	@Inject
	public GroovyConsole(GroovyInterpreter interpreter) {
		super();
		this.interpreter = interpreter;
		interpreter.setCodeCompletion(this);
		this.appendText("Prob 2.0 Groovy Console \n >");
		setListeners();
		InputMap<KeyEvent> ctrlSpace = InputMap.consume(EventPattern.keyPressed(KeyCode.SPACE, KeyCodeCombination.CONTROL_DOWN), e-> this.triggerCodeCompletion(CodeCompletionTriggerAction.TRIGGER));
		Nodes.addInputMap(this, ctrlSpace);
	}
		
	public void reset() {
		this.replaceText("Prob 2.0 Groovy Console");
	}
	
	public void setInterpreter(GroovyInterpreter interpreter) {
		this.interpreter = interpreter;
	}
	
	@Override
	protected void keyPressed(KeyEvent e) {
		if(".".equals(e.getText())) {
			triggerCodeCompletion(CodeCompletionTriggerAction.POINT);
		}
		super.keyPressed(e);
	}
	
	
	protected void setListeners() {
		setCodeCompletionEvent();
		setDragDrop();
	}
		
	private void triggerCodeCompletion(CodeCompletionTriggerAction action) {
		this.replaceText(this.getText());
		if(getCaretPosition() > this.getText().lastIndexOf("\n") + 2) {
			int caretPosInLine = getCurrentLine().length() - (getLength() - getCaretPosition());
			interpreter.triggerCodeCompletion(getCurrentLine().substring(0, caretPosInLine), action);
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
                    this.replaceText(newText);
                    charCounterInLine += path.length();
                    currentPosInLine += path.length();
                    this.moveTo(caretPosition + path.length());
                }
            }
            e.setDropCompleted(success);
            e.consume();
		});
	}
	
	private void handleCodeCompletionEvent(CodeCompletionEvent e) {
		if(e.getCode() == KeyCode.ENTER || e.getEvent() instanceof MouseEvent || ";".equals(((KeyEvent)e.getEvent()).getText())) {
			handleChooseSuggestion(e);
			this.setEstimatedScrollY(Double.MAX_VALUE);
		} else if(((CodeCompletionEvent)e).getCode() == KeyCode.SPACE) {
			//handle Space in Code Completion
			keyPressed((KeyEvent)e.getEvent());
			e.consume();
		}
	}
	
	private void handleChooseSuggestion(CodeCompletionEvent e) {
		String choice = ((CodeCompletionEvent) e).getChoice();
		String suggestion = ((CodeCompletionEvent) e).getCurrentSuggestion();
		String newText = this.getText().substring(0, this.getCaretPosition() - suggestion.length());
		newText = new StringBuilder(newText).append(choice).toString();
		int indexSkipped = getIndexSkipped(this.getText().substring(this.getCaretPosition()), choice, suggestion);
		int indexOfRest = this.getCaretPosition() + indexSkipped;
		newText = new StringBuilder(newText).append(this.getText().substring(indexOfRest)).toString();
		int diff = newText.length() - this.getText().length();
		this.replaceText(newText);
		currentPosInLine += diff + indexSkipped;
		charCounterInLine += diff;
		this.moveTo(indexOfRest + diff);
	}
	
	private int getIndexSkipped(String rest, String choice, String suggestion) {
		String restOfChoice = choice.substring(suggestion.length());
		int result = 0;
		for(int i = 0; i < Math.min(rest.length(), restOfChoice.length()); i++) {
			if(restOfChoice.charAt(i) == rest.charAt(i)) {
				result++;
			} else {
				break;
			}
		}
		return result;
	}
		
	@Override
	protected void handleEnter() {
		super.handleEnterAbstract();
		if(getCurrentLine().isEmpty()) {
			this.appendText("\nnull");
		} else {
			ConsoleInstruction instruction = instructions.get(posInList);
			if("clear".equals(interpreter.exec(instruction).getConsoleOutput())) {
				reset();
			} else {
				this.appendText("\n" + interpreter.exec(instruction));
			}
		}
		this.appendText("\n >");
		this.replaceText(this.getText());
		this.setEstimatedScrollY(Double.MAX_VALUE);
	}
	
		
	public void closeObjectStage() {
		interpreter.closeObjectStage();
	}


}
