package de.prob2.ui.groovy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.inject.Inject;

import de.prob.statespace.AnimationSelector;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;


public class GroovyConsole extends AnchorPane {
	
	private AnimationSelector animations;
	private int charCounterInLine = 0;
	private int currentPosInLine = 0;
	private final KeyCode[] rest = {KeyCode.ESCAPE,KeyCode.SCROLL_LOCK,KeyCode.PAUSE,KeyCode.NUM_LOCK,KeyCode.INSERT,KeyCode.CONTEXT_MENU,KeyCode.CAPS};
	private List<String> instructions;
	private int posInList = 0;
	private String currentLine ="";
	
	@FXML
	private TextArea tagroovy;
	
	@Inject
	private GroovyConsole(FXMLLoader loader, AnimationSelector animations) {
		this.animations = animations;
		try {
			loader.setLocation(getClass().getResource("groovy_console.fxml"));
			loader.setRoot(this);
			loader.setController(this);
			loader.load();
		} catch (IOException e) {
			e.printStackTrace();
		}
		tagroovy.appendText("Prob 2.0 Groovy Console \n >");
		this.instructions = new ArrayList<String>();
		setListeners();

	}
	
	private void setListeners() {
				
		tagroovy.addEventFilter(MouseEvent.ANY, e-> {
			if(e.isMiddleButtonDown()) {
				return;
			}
			tagroovy.deselect();
			goToLastPos();
			
		});
				
		tagroovy.setOnKeyPressed(e-> {
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
				charCounterInLine = 0;
				currentPosInLine = 0;
				posInList++;
				e.consume();
				tagroovy.appendText("\n >");
				instructions.add(currentLine);
				currentLine ="";
				return;
			}
			
			if(handleRest(e)) {
				return;
			}
			
			if(!e.getCode().isFunctionKey() && !e.getCode().isMediaKey() && !e.getCode().isModifierKey()) {
				currentLine += e.getText();
				charCounterInLine++;
				currentPosInLine++;
				return;
			}
		});
	}
	
	private void goToLastPos() {
		tagroovy.setText(tagroovy.getText());
		tagroovy.positionCaret(tagroovy.getLength());
		currentPosInLine = charCounterInLine;
	}
	
	
	private void handleArrowKeys(KeyEvent e) {
		if(e.getCode().equals(KeyCode.LEFT)) {
			if(currentPosInLine > 0) {
				currentPosInLine = Math.max(currentPosInLine - 1, 0);
			} else {
				e.consume();
			}
		} else if(e.getCode().equals(KeyCode.UP) || e.getCode().equals(KeyCode.DOWN)) {
			if(e.getCode().equals(KeyCode.UP)) {
				e.consume();
				if(posInList == 0) { 
					return;
				}
				if(posInList == instructions.size() - 1 && instructions.get(posInList) != currentLine) {
					//String lastinstruction = 
					if(instructions.get(instructions.size()-1).equals("")) {
						instructions.set(instructions.size()-1, currentLine);
					} else {
						instructions.add(currentLine);
					}
				}
				posInList = Math.max(posInList - 1, 0);
			} else {
				if(posInList == instructions.size() - 1) { 
					return;
				}
				posInList = Math.min(posInList+1, instructions.size()-1);
			}
			int posOfEnter = tagroovy.getText().lastIndexOf("\n");
			tagroovy.setText(tagroovy.getText().substring(0, posOfEnter + 3));
			currentLine = instructions.get(posInList);
			charCounterInLine = currentLine.length();
			currentPosInLine = charCounterInLine;
			tagroovy.appendText(currentLine);
		} else if(e.getCode().equals(KeyCode.RIGHT)) {
			if(currentPosInLine < charCounterInLine) {
				currentPosInLine++;
			}
			
		}
	}
	
	private boolean handleRest(KeyEvent e) {
		if(Arrays.asList(rest).contains(e.getCode())) {
			e.consume();
			return true;
		}
		return false;
	}
	

	
	private void handleDeletion(KeyEvent e) {
		if(!tagroovy.getSelectedText().equals("")) {
			e.consume();
			return;
		}
		if(e.getCode().equals(KeyCode.BACK_SPACE)) {
			if(currentPosInLine > 0) {
				currentPosInLine = Math.max(currentPosInLine - 1, 0);
				charCounterInLine = Math.max(charCounterInLine - 1, 0);		
			} else {
				e.consume();
			}
		} else {
			if(currentPosInLine < charCounterInLine) {
				charCounterInLine = Math.max(charCounterInLine - 1, 0);
			}
		}
		int posOfEnter = tagroovy.getText().lastIndexOf("\n");
		if(posOfEnter + 3 > tagroovy.getText().length()-1) {
			return;
		}
		currentLine = tagroovy.getText().substring(posOfEnter + 3, tagroovy.getText().length()-1);
	}

	
}
