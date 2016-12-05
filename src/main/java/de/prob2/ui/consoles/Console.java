package de.prob2.ui.consoles;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import javafx.scene.control.IndexRange;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;

import org.fxmisc.richtext.StyleClassedTextArea;
import org.fxmisc.wellbehaved.event.EventPattern;
import org.fxmisc.wellbehaved.event.InputMap;
import org.fxmisc.wellbehaved.event.Nodes;


public abstract class Console extends StyleClassedTextArea {
	private static final Set<KeyCode> REST = EnumSet.of(KeyCode.ESCAPE, KeyCode.SCROLL_LOCK, KeyCode.PAUSE, KeyCode.NUM_LOCK, KeyCode.INSERT, KeyCode.CONTEXT_MENU, KeyCode.CAPS, KeyCode.TAB, KeyCode.ALT);
	
	protected List<ConsoleInstruction> instructions;
	protected int charCounterInLine = 0;
	protected int currentPosInLine = 0;
	protected int posInList = -1;
	protected ConsoleSearchHandler searchHandler;
	protected List<IndexRange> errors;
	protected Executable interpreter;

	protected Console() {
		this.instructions = new ArrayList<>();
		this.errors = new ArrayList<>();
		this.searchHandler = new ConsoleSearchHandler(this, instructions);
		this.requestFollowCaret();
		setEvents();
	}
	
	public void setEvents() {
		Nodes.addInputMap(this, InputMap.consume(EventPattern.mouseClicked(MouseButton.PRIMARY), e->  this.mouseClicked()));
		Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(), this::keyPressed));
		
		// GUI-style shortcuts, these should use the Shortcut key (i. e. Command on Mac, Control on other systems).
		Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(KeyCode.C, KeyCombination.SHORTCUT_DOWN), e-> this.copy()));
		Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(KeyCode.V, KeyCombination.SHORTCUT_DOWN), e-> this.paste()));
		
		// Shell/Emacs-style shortcuts, these should always use Control as the modifier, even on Mac (this is how it works in a normal terminal window).
		Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(KeyCode.R, KeyCombination.CONTROL_DOWN), e-> this.controlR()));
		Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(KeyCode.A, KeyCombination.CONTROL_DOWN), e-> this.controlA()));
		Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(KeyCode.E, KeyCombination.CONTROL_DOWN), e-> this.controlE()));
				
		Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(KeyCode.UP), e-> this.handleUp()));
		Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(KeyCode.DOWN), e-> this.handleDown()));
		Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(KeyCode.LEFT), e-> this.handleLeft()));
		Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(KeyCode.RIGHT), e-> this.handleRight()));
		Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(KeyCode.DELETE), this::handleDeletion));
		Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(KeyCode.BACK_SPACE), this::handleDeletion));
		Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(KeyCode.ENTER, KeyCombination.SHIFT_DOWN), KeyEvent::consume));
		Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(KeyCode.ESCAPE, KeyCombination.SHIFT_DOWN), KeyEvent::consume));
		Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(KeyCode.ENTER, KeyCombination.ALT_DOWN), KeyEvent::consume));
		Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(KeyCode.BACK_SPACE, KeyCombination.ALT_DOWN), KeyEvent::consume));
		Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(KeyCode.DELETE, KeyCombination.ALT_DOWN), KeyEvent::consume));
		Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(KeyCode.ENTER), e-> this.handleEnter()));
	}
		
	@Override
	public void paste() {
		if(searchHandler.isActive()) {
			return;
		}
		if(this.getLength() - 1 - this.getCaretPosition() >= charCounterInLine) {
			goToLastPos();
		}
		String oldText = this.getText();
		int posOfEnter = oldText.lastIndexOf('\n');
		super.paste();
		int diff = this.getLength() - oldText.length();
		String currentLine = this.getText().substring(posOfEnter + 3, this.getText().length());
		if(currentLine.contains("\n")) {
			this.deleteText(this.getText().length() - currentLine.length(), this.getText().length());
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
				
	private void mouseClicked() {
		if(this.getLength() - 1 - this.getCaretPosition() < charCounterInLine) {
			currentPosInLine = charCounterInLine - (this.getLength() - this.getCaretPosition());
		}
	}
	
	public void controlR() {
		if(!searchHandler.isActive()) {
			activateSearch();
		} else {
			searchHandler.searchNext();
		}
	}
	
	protected void keyPressed(KeyEvent e) {
		if(REST.contains(e.getCode())) {
			return;
		}
		if(!e.getCode().isFunctionKey() && !e.getCode().isMediaKey() && !e.getCode().isModifierKey()) {
			handleInsertChar(e);
		}
	}
	
	private void handleInsertChar(KeyEvent e) {
		if(this.getLength() - this.getCaretPosition() > charCounterInLine) {
			goToLastPos();
		}
		if (e.isControlDown() || e.isMetaDown() || e.getText().isEmpty()) {
			return;
		}
		charCounterInLine++;
		currentPosInLine++;
		posInList = instructions.size() - 1;
		searchHandler.handleKey(e);
	}
	
	private void controlA() {
		if(!searchHandler.isActive()) {
			this.moveTo(this.getCaretPosition() - currentPosInLine);
			currentPosInLine = 0;
		}
	}
	
	private void controlE() {
		if(!searchHandler.isActive()) {
			this.moveTo(this.getLength());
			currentPosInLine = charCounterInLine;
		}
	}

	protected void activateSearch() {
		int posOfEnter = this.getText().lastIndexOf('\n');
		this.deleteText(posOfEnter + 1, this.getText().length());
		this.appendText(ConsoleSearchHandler.FOUND + getCurrentLine());
		this.moveTo(this.getText().lastIndexOf('\''));
		currentPosInLine = 0;
		charCounterInLine = 0;
		searchHandler.activateSearch();
	}
	
	protected void deactivateSearch() {
		if(searchHandler.isActive()) {
			int posOfEnter = this.getText().lastIndexOf('\n');
			String searchResult = searchHandler.getCurrentSearchResult();
			this.deleteText(posOfEnter + 1, this.getText().length());
			this.appendText(" >" + searchResult);
			this.moveTo(this.getText().length());
			charCounterInLine = searchResult.length();
			currentPosInLine = charCounterInLine;
			searchHandler.deactivateSearch();
		}
	}
		
	private void goToLastPos() {
		this.moveTo(this.getLength());
		deselect();
		currentPosInLine = charCounterInLine;
	}
	
	protected abstract void reset();
		
	protected void handleEnter() {
		charCounterInLine = 0;
		currentPosInLine = 0;
		String currentLine = getCurrentLine();
		if(searchHandler.isActive()) {
			currentLine = searchHandler.getCurrentSearchResult();
		}
		if(!currentLine.isEmpty()) {
			if(!instructions.isEmpty() && instructions.get(instructions.size() - 1).getOption() != ConsoleInstructionOption.ENTER) {
				instructions.set(instructions.size() - 1, new ConsoleInstruction(currentLine, ConsoleInstructionOption.ENTER));
			} else {
				instructions.add(new ConsoleInstruction(currentLine, ConsoleInstructionOption.ENTER));
			}
			posInList = instructions.size() - 1;
			ConsoleInstruction instruction = instructions.get(posInList);
			ConsoleExecResult execResult = interpreter.exec(instruction);
			if("clear".equals(execResult.getConsoleOutput())) {
				reset();
			} else {
				this.appendText("\n" + execResult);
			}
			if(execResult.getResultType() == ConsoleExecResultType.ERROR) {
				int begin = this.getText().length() - execResult.toString().length();
				int end = this.getText().length();
				this.setStyleClass(begin, end, "error");
				errors.add(new IndexRange(begin, end));
			}
		} else {
			this.appendText("\nnull");
		}
		searchHandler.handleEnter();
		this.appendText("\n >");
		this.setStyleClass(this.getText().length() - 2, this.getText().length(), "current");
		this.setEstimatedScrollY(Double.MAX_VALUE);
		goToLastPos();
	}
		
	private void handleDown() {
		deactivateSearch();
		if(posInList == instructions.size() - 1) {
			return;
		}
		posInList = Math.min(posInList+1, instructions.size() - 1);
		setTextAfterArrowKey();
	}
	
	private void handleUp() {
		deactivateSearch();
		if(posInList == -1) { 
			return;
		}
		if(posInList == instructions.size() - 1) {
			String lastinstruction = instructions.get(instructions.size()-1).getInstruction();
			if(!lastinstruction.equals(getCurrentLine()) && posInList == instructions.size() - 1) {
				if(instructions.get(posInList).getOption() == ConsoleInstructionOption.UP) {
					instructions.set(instructions.size() - 1, new ConsoleInstruction(getCurrentLine(), ConsoleInstructionOption.UP));
				} else {
					instructions.add(new ConsoleInstruction(getCurrentLine(), ConsoleInstructionOption.UP));
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
		if(currentPosInLine > 0 && this.getLength() - this.getCaretPosition() <= charCounterInLine) {
			currentPosInLine--;
			this.moveTo(this.getCaretPosition() - 1);
		} else if(currentPosInLine == 0) {
			super.deselect();
		}
	}
	
	private void handleRight() {
		deactivateSearch();
		if(currentPosInLine < charCounterInLine && this.getLength() - this.getCaretPosition() <= charCounterInLine) {		
			currentPosInLine++;
			this.moveTo(this.getCaretPosition() + 1);
		}
	}
	
	
	private void setTextAfterArrowKey() {
		int posOfEnter = this.getText().lastIndexOf('\n');
		String currentLine = instructions.get(posInList).getInstruction();
		this.deleteText(posOfEnter + 3, this.getText().length());
		this.appendText(currentLine);
		charCounterInLine = currentLine.length();
		currentPosInLine = charCounterInLine;
		this.setEstimatedScrollY(Double.MAX_VALUE);
	}
	
	private void handleDeletion(KeyEvent e) {
		int maxPosInLine = charCounterInLine;
		if(searchHandler.handleDeletion(e)) {
			return;
		}
		if(searchHandler.isActive()) {
			maxPosInLine = charCounterInLine + 2 + searchHandler.getCurrentSearchResult().length();
		}
		if(!this.getSelectedText().isEmpty() || this.getLength() - this.getCaretPosition() > maxPosInLine) {
			return;
		}
		if(e.getCode().equals(KeyCode.BACK_SPACE)) {
			handleBackspace();
		} else {
			handleDelete();
		}
	}
	
	private void handleBackspace() {
		if(currentPosInLine > 0) {
			currentPosInLine = Math.max(currentPosInLine - 1, 0);
			charCounterInLine = Math.max(charCounterInLine - 1, 0);	
			this.deletePreviousChar();
		}
	}
	
	private void handleDelete() {
		if(currentPosInLine < charCounterInLine) {
			charCounterInLine = Math.max(charCounterInLine - 1, 0);
			this.deleteNextChar();
		}
	}
	
	public String getCurrentLine() {
		if(this.getText(this.getParagraphs().size() - 1).length() < 2) {
			return "";
		}
		return this.getText(this.getParagraphs().size() - 1).substring(2);
	}
		
	public int getCurrentPosInLine() {
		return currentPosInLine;
	}
	
	public int getCharCounterInLine() {
		return charCounterInLine;
	}
	
	public List<ConsoleInstruction> getInstructions() {
		return instructions;
	}
	
	public List<String> getInstructionEntries() {
		//last 100 Entries
		List<String> entries = new ArrayList<>();
		for(int i = Math.max(instructions.size() - 100,0); i < instructions.size(); i++) {
			entries.add(instructions.get(i).getInstruction());
		}
		return entries;
	}
	
	public void increaseCounter() {
		posInList++;
	}
	
	public void setCharCounterInLine(int value) {
		charCounterInLine = value;
	}
	
	public void setCurrentPosInLine(int value) {
		currentPosInLine = value;
	}
	
	public List<IndexRange> getErrors() {
		return errors;
	}
	
}
