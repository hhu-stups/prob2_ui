package de.prob2.ui.groovy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;

public class GroovyConsole extends TextArea {
	
	private int charCounterInLine = 0;
	private int currentPosInLine = 0;
	private final KeyCode[] rest = {KeyCode.ESCAPE,KeyCode.SCROLL_LOCK,KeyCode.PAUSE,KeyCode.NUM_LOCK,KeyCode.INSERT,KeyCode.CONTEXT_MENU,KeyCode.CAPS};
	private List<String> instructions;
	private int posInList = -1;
	private int numberOfInstructions = 0;
	private String currentLine ="";
	private GroovyInterpreter interpreter;
	

	public GroovyConsole() {
		super();
		this.appendText("Prob 2.0 Groovy Console \n >");
		this.instructions = new ArrayList<String>();
		setListeners();
	}
	
	public void setInterpreter(GroovyInterpreter interpreter) {
		this.interpreter = interpreter;
	}
	
	@Override
	public void paste() {
		int oldlength = this.getText().length();
		int posOfEnter = this.getText().lastIndexOf("\n"); 
		super.paste();
		int diff = this.getText().length() - oldlength - 1;
		currentLine = this.getText().substring(posOfEnter + 3, this.getText().length());
		charCounterInLine += diff;
		currentPosInLine += diff;
	}
	
	private void setListeners() {
		
		KeyCombination paste = new KeyCodeCombination(KeyCode.V, KeyCodeCombination.CONTROL_ANY);
		this.addEventHandler(KeyEvent.KEY_PRESSED, e-> {
			if(paste.match(e)) {
				return;
			}
		});
		
		this.addEventFilter(MouseEvent.ANY, e-> {
			if(e.isMiddleButtonDown()) {
				return;
			}
			this.deselect();
			goToLastPos();
		});
		
				
		this.setOnKeyPressed(e-> {
			
			if(e.getCode().isArrowKey()) {
				handleArrowKeys(e);
				return;
			}
			if(e.getCode().isNavigationKey()) {
				e.consume();
				return;
			}
			if(e.getCode().equals(KeyCode.BACK_SPACE) || e.getCode().equals(KeyCode.DELETE)) {
				handleDeletion(e);
				return;
			}
			if(e.getCode().equals(KeyCode.ENTER)) {
				handleEnter(e);
				return;
			}
			
			if(!e.getCode().isFunctionKey() && !e.getCode().isMediaKey() && !e.getCode().isModifierKey()) {
				handleInsertChar(e);
				return;
			}
			
			if(handleRest(e)) {
				return;
			}

		});
	}
	
	private void goToLastPos() {
		this.setText(this.getText());
		this.positionCaret(this.getLength());
		currentPosInLine = charCounterInLine;
	}
	
	private void handleInsertChar(KeyEvent e) {
		if(e.getText().equals("")) {
			return;
		}
		currentLine = new StringBuilder(currentLine).insert(currentPosInLine, e.getText().charAt(0)).toString();
		charCounterInLine++;
		currentPosInLine++;
		posInList = instructions.size() - 1;
	}
	
	private void handleEnter(KeyEvent e) {
		charCounterInLine = 0;
		currentPosInLine = 0;
		e.consume();
		
		if(instructions.size() == 0) {
			if(!currentLine.equals("")) {
				instructions.add(currentLine);
				numberOfInstructions++;
			}
		} else {
			//add Instruction if last Instruction is not "", otherwise replace it
			String lastinstruction = instructions.get(instructions.size()-1);
			if(!(lastinstruction.equals(""))) {
				instructions.add(currentLine);
				numberOfInstructions++;
			} else if(!currentLine.equals("")) {
				instructions.set(instructions.size() - 1, currentLine);
			}
		}
		posInList = instructions.size() - 1;
		currentLine = "";
		this.appendText("\n" + interpreter.exec(instructions.get(instructions.size() - 1)));
		this.appendText("\n >");
		
	}	
	
	private void handleArrowKeys(KeyEvent e) {
		if(e.getCode().equals(KeyCode.LEFT)) {
			handleLeft(e);
		} else if(e.getCode().equals(KeyCode.UP) || e.getCode().equals(KeyCode.DOWN)) {
			boolean needReturn;
			if(e.getCode().equals(KeyCode.UP)) {
				needReturn = handleUp(e);
			} else {
				needReturn = handleDown(e);				
			}
			if(needReturn) {
				return;
			}
			setTextAfterArrowKey();
		} else if(e.getCode().equals(KeyCode.RIGHT)) {
			handleRight(e);
		}
	}
	
	private boolean handleUp(KeyEvent e) {
		e.consume();
		
		if(posInList == -1) { 
			return true;
		}
		if(posInList == instructions.size() - 1) {
			String lastinstruction = instructions.get(instructions.size()-1);
			if(!lastinstruction.equals("")) {
				if(!lastinstruction.equals(currentLine)) {
					if(posInList == instructions.size() - 1) {
						instructions.add(currentLine);
						setTextAfterArrowKey();
						return true;
					} else {
						instructions.set(numberOfInstructions, currentLine);
					}
				}
			} else {
				instructions.set(instructions.size() - 1, currentLine);
			}
		}
		posInList = Math.max(posInList - 1, 0);
		return false;
	}
	
	private boolean handleDown(KeyEvent e) {
		if(posInList == instructions.size() - 1) {
			return true;
		}
		posInList = Math.min(posInList+1, instructions.size() - 1);
		return false;
	}
	
	private void handleLeft(KeyEvent e) {
		//handleLeft
		if(currentPosInLine > 0) {
			currentPosInLine = Math.max(currentPosInLine - 1, 0);
		} else {
			e.consume();
		}
	}
	
	private void handleRight(KeyEvent e) {
		if(currentPosInLine < charCounterInLine) {
			currentPosInLine++;
		}
	}
	
	private void setTextAfterArrowKey() {
		int posOfEnter = this.getText().lastIndexOf("\n");
		this.setText(this.getText().substring(0, posOfEnter + 3));
		currentLine = instructions.get(posInList);
		charCounterInLine = currentLine.length();
		currentPosInLine = charCounterInLine;
		this.appendText(currentLine);
	}
	
	private boolean handleRest(KeyEvent e) {
		if(Arrays.asList(rest).contains(e.getCode())) {
			e.consume();
			return true;
		}
		return false;
	}
	
	private void handleDeletion(KeyEvent e) {
		boolean needReturn = false;
		if(!this.getSelectedText().equals("")) {
			e.consume();
			return;
		}
		if(e.getCode().equals(KeyCode.BACK_SPACE)) {
			needReturn = handleBackspace(e);
			if(needReturn) {
				return;
			}
		} else {
			needReturn = handleDelete(e);
			if(needReturn) {
				return;
			}
		}
		updateTextAreaAfterDeletion();
	}
	
	private boolean handleBackspace(KeyEvent e) {
		if(currentPosInLine > 0) {
			currentPosInLine = Math.max(currentPosInLine - 1, 0);
			charCounterInLine = Math.max(charCounterInLine - 1, 0);		
		} else {
			e.consume();
			return true;
		}
		return false;
	}
	
	private boolean handleDelete(KeyEvent e) {
		if(currentPosInLine < charCounterInLine) {
			charCounterInLine = Math.max(charCounterInLine - 1, 0);
		} else if(currentPosInLine == charCounterInLine) {
			return true;
		}
		return false;
	}
	
	private void updateTextAreaAfterDeletion() {
		int posOfEnter = this.getText().lastIndexOf("\n");
		currentLine = this.getText().substring(posOfEnter + 3, posOfEnter + 3 + currentPosInLine);
		currentLine += this.getText().substring(posOfEnter+ 4 + currentPosInLine, this.getText().length());
	}

}
