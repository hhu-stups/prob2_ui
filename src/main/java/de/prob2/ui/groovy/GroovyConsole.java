package de.prob2.ui.groovy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
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
		this.instructions = new ArrayList<String>();
		this.appendText("Prob 2.0 Groovy Console \n >");
		setListeners();
	}
	
	public void setInterpreter(GroovyInterpreter interpreter) {
		this.interpreter = interpreter;
	}
	
	@Override
	public void paste() {
		if(this.getLength() - this.getCaretPosition() > charCounterInLine) {
			goToLastPos();
		}
		int oldlength = this.getText().length();
		int posOfEnter = this.getText().lastIndexOf("\n");
		super.paste();
		int diff = this.getText().length() - oldlength;
		currentLine = this.getText().substring(posOfEnter + 3, this.getText().length());
		charCounterInLine += diff;
		currentPosInLine += diff;
		correctPosInLine();
	}
	
	@Override
	public void copy() {
		super.copy();
		correctPosInLine();
		goToLastPos();
	}
	
	@Override
	public void cut() {
		super.cut();
		correctPosInLine();
	}	
	
	private void correctPosInLine() {
		if(charCounterInLine > 0) {
			charCounterInLine--;
			currentPosInLine--;
		}
	}
	
	@Override
	public void selectForward() {
		//do nothing, but stay at correct position
		if(currentPosInLine != charCounterInLine) {
			currentPosInLine--;
		}
	}
	
	@Override
	public void selectBackward() {
		//do nothing, but stay at correct position
		if(currentPosInLine != 0) {
			currentPosInLine++;
		}
	}	
	
	//Undo
	private void setListeners() {
				
		this.setOnKeyPressed(e-> {
			if(e.getCode().isArrowKey()) {
				handleArrowKeys(e);
				this.setScrollTop(Double.MAX_VALUE);
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
		this.positionCaret(this.getLength());
		currentPosInLine = charCounterInLine;
	}
	
	private void handleInsertChar(KeyEvent e) {
		if(e.getText().equals("") || (!e.isControlDown() && (this.getLength() - this.getCaretPosition()) > charCounterInLine)) {
			goToLastPos();
			if(e.getText().equals("")) {
				e.consume();
				return;
			}
		}
		currentLine = new StringBuilder(currentLine).insert(currentPosInLine, e.getText()).toString();
		charCounterInLine++;
		currentPosInLine++;
		posInList = instructions.size() - 1;

	}
	
	private void handleEnter(KeyEvent e) {
		charCounterInLine = 0;
		currentPosInLine = 0;
		e.consume();
		
		if(instructions.size() == 0) {
			instructions.add(currentLine);
			numberOfInstructions++;
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
		this.appendText("\n" + interpreter.exec(instructions.get(posInList)));
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
		e.consume();
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
		if(!this.getSelectedText().equals("") || this.getLength() - this.getCaretPosition() > charCounterInLine || e.isControlDown()) {
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
