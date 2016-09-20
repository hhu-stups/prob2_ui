package de.prob2.ui.groovy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;


public class GroovyConsole extends TextArea {
	
	private int charCounterInLine = 0;
	private int currentPosInLine = 0;
	private static final KeyCode[] REST = {KeyCode.ESCAPE,KeyCode.SCROLL_LOCK,KeyCode.PAUSE,KeyCode.NUM_LOCK,KeyCode.INSERT,KeyCode.CONTEXT_MENU,KeyCode.CAPS};
	private List<Instruction> instructions;
	private int posInList = -1;
	private GroovyInterpreter interpreter;
	

	public GroovyConsole() {
		super();
		this.setContextMenu(new ContextMenu());
		this.instructions = new ArrayList<>();
		this.appendText("Prob 2.0 Groovy Console \n >");
		setListeners();
	}
	
	public void setInterpreter(GroovyInterpreter interpreter) {
		this.interpreter = interpreter;
	}
	
	
	@Override
	public void paste() {
		if(this.getLength() - 1 - this.getCaretPosition() >= charCounterInLine) {
			goToLastPos();
		}
		String oldText = this.getText();
		int posOfEnter = oldText.lastIndexOf('\n');
		super.paste();
		int diff = this.getLength() - oldText.length();
		String currentLine = this.getText().substring(posOfEnter + 3, this.getText().length());
		if(currentLine.contains("\n")) {
			this.setText(oldText);
			goToLastPos();
			return;
		}
		charCounterInLine += diff;
		currentPosInLine += diff;
	}
	
	@Override
	public void copy() {
		super.copy();
		goToLastPos();
	}
		
	@Override
	public void cut() {
		super.cut();
	}
	
		
	@Override
	public void forward() {
		if(currentPosInLine < charCounterInLine && this.getLength() - 1 - this.getCaretPosition() <= charCounterInLine) {
			currentPosInLine++;
			super.forward();
			this.setScrollTop(Double.MIN_VALUE);
		}
	}
	
	@Override
	public void backward() {
		//handleLeft
		if(currentPosInLine > 0 && this.getLength() - 1 - this.getCaretPosition() <= charCounterInLine) {
			currentPosInLine = Math.max(currentPosInLine - 1, 0);
			super.backward();
			this.setScrollTop(Double.MIN_VALUE);
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
	
	//
	private void setListeners() {
		this.addEventFilter(KeyEvent.ANY, e-> {
			if(e.getCode() == KeyCode.Z && (e.isShortcutDown() || e.isAltDown())) {
				e.consume();
			}
		});
		
		this.addEventFilter(MouseEvent.ANY, e-> {
			if(e.getButton() == MouseButton.PRIMARY) {
				if(this.getLength() - 1 - this.getCaretPosition() < charCounterInLine) {
					currentPosInLine = charCounterInLine - (this.getLength() - this.getCaretPosition());
				}
			}
		});
		
		this.setOnKeyPressed(e -> {
			if (e.getCode().equals(KeyCode.UP) || e.getCode().equals(KeyCode.DOWN)) {
				handleArrowKeys(e);
				this.setScrollTop(Double.MAX_VALUE);
			} else if (e.getCode().isArrowKey()) {
			} else if (e.getCode().isNavigationKey()) {
				e.consume();
			} else if (e.getCode().equals(KeyCode.BACK_SPACE) || e.getCode().equals(KeyCode.DELETE)) {
				handleDeletion(e);
			} else if (e.getCode().equals(KeyCode.ENTER)) {
				handleEnter(e);
			} else if (!e.getCode().isFunctionKey() && !e.getCode().isMediaKey() && !e.getCode().isModifierKey()) {
				handleInsertChar(e);
			} else if (handleRest(e)) {
			}
		});
	}
	
	private void goToLastPos() {
		this.positionCaret(this.getLength());
		currentPosInLine = charCounterInLine;
	}
	
	private void handleInsertChar(KeyEvent e) {
		if(e.getText().isEmpty() || (!(e.isShortcutDown() || e.isAltDown()) && (this.getLength() - this.getCaretPosition()) > charCounterInLine)) {
			if(!(e.getCode().equals(KeyCode.UNDEFINED) || e.getCode().equals(KeyCode.ALT_GRAPH))) {
				goToLastPos();
			}
			if(e.getText().isEmpty()) {
				e.consume();
				return;
			}
		}
	
		if (e.isShortcutDown() || e.isAltDown()) {
			return;
		}
		charCounterInLine++;
		currentPosInLine++;
		posInList = instructions.size() - 1;

	}
	
	private void handleEnter(KeyEvent e) {
		charCounterInLine = 0;
		currentPosInLine = 0;
		e.consume();
		if(getCurrentLine().equals("")) {
			this.appendText("\n" + "null");
		} else {
			Instruction lastInstruction = null;
			if(!instructions.isEmpty()) {
				lastInstruction = instructions.get(instructions.size() - 1);
			}
			// FIXME The logic is confusing! Please refactor!
			if(instructions.isEmpty() || lastInstruction.getOption() == InstructionOption.ENTER) {
				instructions.add(new Instruction(getCurrentLine(), InstructionOption.ENTER));
			} else {
				instructions.set(instructions.size() - 1, new Instruction(getCurrentLine(), InstructionOption.ENTER));
			}
			posInList = instructions.size() - 1;
			this.appendText("\n" + interpreter.exec(instructions.get(posInList)));
		}
		this.appendText("\n >");
	}
	

	
	private void handleArrowKeys(KeyEvent e) {
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
	}
	
	private boolean handleUp(KeyEvent e) {
		e.consume();
		if(posInList == -1) { 
			return true;
		}
		if(posInList == instructions.size() - 1) {
			String lastinstruction = instructions.get(instructions.size()-1).getInstruction();
			if(!lastinstruction.equals(getCurrentLine()) && posInList == instructions.size() - 1) {
				if(instructions.get(posInList).getOption() == InstructionOption.UP) {
					instructions.set(instructions.size() - 1, new Instruction(getCurrentLine(), InstructionOption.UP));
				} else {
					instructions.add(new Instruction(getCurrentLine(), InstructionOption.UP));
					setTextAfterArrowKey();
					return true;
				}
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
	
	private void setTextAfterArrowKey() {
		int posOfEnter = this.getText().lastIndexOf("\n");
		this.setText(this.getText().substring(0, posOfEnter + 3));
		String currentLine = instructions.get(posInList).getInstruction();
		charCounterInLine = currentLine.length();
		currentPosInLine = charCounterInLine;
		this.appendText(currentLine);
	}
	
	private boolean handleRest(KeyEvent e) {
		if(Arrays.asList(REST).contains(e.getCode())) {
			e.consume();
			return true;
		}
		return false;
	}
	
	private void handleDeletion(KeyEvent e) {
		boolean needReturn;
		if(!this.getSelectedText().isEmpty() || this.getLength() - this.getCaretPosition() > charCounterInLine || e.isShortcutDown() || e.isAltDown()) {
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
	
	private String getCurrentLine() {
		int posOfEnter = this.getText().lastIndexOf("\n");
		return this.getText().substring(posOfEnter + 3, this.getText().length());
	}
		
	public void closeObjectStage() {
		interpreter.closeObjectStage();
	}

}
