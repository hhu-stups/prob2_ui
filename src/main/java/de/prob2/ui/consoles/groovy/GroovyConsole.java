package de.prob2.ui.consoles.groovy;

import java.io.File;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.consoles.Console;
import de.prob2.ui.consoles.groovy.codecompletion.CodeCompletionEvent;
import de.prob2.ui.consoles.groovy.codecompletion.CodeCompletionTriggerAction;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;

import org.fxmisc.wellbehaved.event.EventPattern;
import org.fxmisc.wellbehaved.event.InputMap;
import org.fxmisc.wellbehaved.event.Nodes;

@Singleton
public class GroovyConsole extends Console {
			
	@Inject
	public GroovyConsole(GroovyInterpreter interpreter) {
		super();
		this.interpreter = interpreter;
		interpreter.setCodeCompletion(this);
		this.appendText("ProB 2.0 Groovy Console \n >");
		setListeners();
		Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(KeyCode.SPACE, KeyCombination.CONTROL_DOWN), e-> this.triggerCodeCompletion(CodeCompletionTriggerAction.TRIGGER)));
	}
	
	public void reset() {
		this.replaceText("ProB 2.0 Groovy Console");
		this.errors.clear();
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
		if(getCaretPosition() > this.getText().lastIndexOf('\n') + 2) {
			int caretPosInLine = getCurrentLine().length() - (getLength() - getCaretPosition());
			((GroovyInterpreter) interpreter).triggerCodeCompletion(getCurrentLine().substring(0, caretPosInLine), action);
		}
	}
	
	private void setCodeCompletionEvent() {
		this.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> ((GroovyInterpreter) interpreter).triggerCloseCodeCompletion());
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
			int lastPosOfEnter = this.getText().lastIndexOf('\n');
			if (dragbord.hasFiles() && this.getCaretPosition() >= lastPosOfEnter + 3) {
				success = true;
				String path = null;
				for (File file : dragbord.getFiles()) {
					path = file.getAbsolutePath();
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
	
	private void handleCodeCompletionEvent(CodeCompletionEvent e) {
		if(e.getCode() == KeyCode.ENTER || e.getEvent() instanceof MouseEvent || ";".equals(((KeyEvent)e.getEvent()).getText())) {
			handleChooseSuggestion(e);
			this.setEstimatedScrollY(Double.MAX_VALUE);
		} else if(e.getCode() == KeyCode.SPACE) {
			//handle Space in Code Completion
			keyPressed((KeyEvent)e.getEvent());
			e.consume();
		}
	}
	
	private void handleChooseSuggestion(CodeCompletionEvent e) {
		String choice = e.getChoice();
		String suggestion = e.getCurrentSuggestion();
		int indexSkipped = getIndexSkipped(this.getText().substring(this.getCaretPosition()), choice, suggestion);
		int indexOfRest = this.getCaretPosition() + indexSkipped;
		int oldLength = this.getText().length();
		String addition = choice + this.getText().substring(indexOfRest);
		this.deleteText(this.getCaretPosition() - suggestion.length(), this.getText().length());
		this.appendText(addition);
		int diff = this.getText().length() - oldLength;
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
		
	public void closeObjectStage() {
		((GroovyInterpreter) interpreter).closeObjectStage();
	}


}
