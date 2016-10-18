package de.prob2.ui.consoles.groovy.codecompletion;

import java.io.IOException;
import javax.script.ScriptEngine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.prob2.ui.consoles.groovy.GroovyConsole;
import de.prob2.ui.consoles.groovy.objects.GroovyAbstractItem;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Point2D;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Popup;

public class GroovyCodeCompletion extends Popup {
	private static final Logger logger = LoggerFactory.getLogger(GroovyCodeCompletion.class);
	
	@FXML
	private ListView<GroovyAbstractItem> lvSuggestions;
	
	private final ObservableList<GroovyAbstractItem> suggestions;
	
	private ScriptEngine engine;
		
	private GroovyConsole parent;
		
	private String currentSuggestion;
	
	private int currentPosInSuggestion;
	
	private int charCounterInSuggestion;
	
	private final GroovyCodeCompletionHandler completionHandler;
	
	public GroovyCodeCompletion(FXMLLoader loader, ScriptEngine engine) {
		loader.setLocation(getClass().getResource("groovy_codecompletion_popup.fxml"));
		loader.setRoot(this);
		loader.setController(this);
		try {
			loader.load();
		} catch (IOException e) {
			logger.error("loading fxml failed", e);
		}
		this.engine = engine;
		this.parent = null;
		this.currentSuggestion = "";
		this.currentPosInSuggestion = 0;
		this.charCounterInSuggestion = 0;
		suggestions = FXCollections.observableArrayList();
		lvSuggestions.setItems(suggestions);
		this.completionHandler = new GroovyCodeCompletionHandler(suggestions);
		setListeners();
	}
		
	public void activate(GroovyConsole console, String currentLine, TriggerAction action) {
		this.parent = console;
		String currentPrefix = currentLine;
		if(action == TriggerAction.TRIGGER) {
			currentPrefix = handleActivationByTriggering(currentLine);
		}
		completionHandler.handleMethodsFromObjects(currentPrefix, currentSuggestion, action, parent, engine);
		completionHandler.handleStaticClasses(currentPrefix, currentSuggestion, action, parent);
		completionHandler.handleObjects(currentSuggestion, action, engine);
		showPopup(console);
	}
	
	private String handleActivationByTriggering(String currentLine) {
		String currentPrefix = currentLine;
		String newCurrentLine = currentLine.replaceAll("\\s","");
		int indexOfPoint = newCurrentLine.lastIndexOf('.');
		int indexOfSemicolon = newCurrentLine.lastIndexOf(';');
		
		if((indexOfPoint < indexOfSemicolon && indexOfSemicolon > getParent().getCurrentPosInLine()) || (indexOfPoint != -1 && indexOfSemicolon == -1)) {
			int index = Math.max(-1, indexOfPoint);
			currentSuggestion = newCurrentLine.substring(index + 1, newCurrentLine.length());
			currentPosInSuggestion = currentSuggestion.length();
			charCounterInSuggestion = currentPosInSuggestion;
			currentPrefix = newCurrentLine.substring(0, index + 1);
		} else {
			int index = Math.max(-1, indexOfSemicolon);
			currentSuggestion = newCurrentLine.substring(index + 1, newCurrentLine.length());
			charCounterInSuggestion = currentSuggestion.length();
			currentPosInSuggestion = charCounterInSuggestion;
			currentPrefix = currentSuggestion;
		}
		return currentPrefix;
	}
	
	private void showPopup(GroovyConsole console) {
		if(suggestions.isEmpty()) {
			return;
		}
		sortSuggestions();
		lvSuggestions.getSelectionModel().selectFirst();
		Point2D point = CaretFinder.findCaretPosition(CaretFinder.findCaret(console));
		double x = point.getX() + 10;
		double y = point.getY() + 20;
		this.show(console, x, y);	
	}
	
	private void sortSuggestions() {
		suggestions.sort((o1,o2) -> o1.toString().compareToIgnoreCase(o2.toString()));
	}
	
	public void deactivate() {
		suggestions.clear();
		completionHandler.clear();
		currentSuggestion ="";
		currentPosInSuggestion = 0;
		charCounterInSuggestion = 0;
		this.hide();
	}
		
	public void filterSuggestions(String addition, CodeCompletionAction action) {
		String currentInstruction = currentSuggestion;
		if(action.equals(CodeCompletionAction.ARROWKEY)) {
			//handle Arrow Key
			currentInstruction = currentSuggestion.substring(0, currentPosInSuggestion);
		} else if(action.equals(CodeCompletionAction.INSERTION)) {
			currentInstruction = handleInsertChar(addition);
		}
		completionHandler.refresh(currentInstruction);
		sortSuggestions();
		lvSuggestions.getSelectionModel().selectFirst();
		if(suggestions.isEmpty()) {
			this.deactivate();
		}
	}
	
	private String handleInsertChar(String addition) {
		currentSuggestion = new StringBuilder(currentSuggestion).insert(currentPosInSuggestion, addition.charAt(0)).toString();
		currentPosInSuggestion++;
		charCounterInSuggestion++;
		return currentSuggestion;
	}
	

	private void setListeners() {
		lvSuggestions.setOnMouseClicked(this::chooseMethod);
		lvSuggestions.setOnKeyPressed(e-> {
			if(e.getCode().equals(KeyCode.SPACE)) {
				getParent().fireEvent(new CodeCompletionEvent(e));
			}
			if(";".equals(e.getText()) || e.getCode().equals(KeyCode.ENTER)) {
				//handle Enter in Groovy Code Completion
				chooseMethod(e);
				return;
			}
			if(e.getCode().equals(KeyCode.LEFT) || e.getCode().equals(KeyCode.RIGHT) || e.getCode().equals(KeyCode.UP) || e.getCode().equals(KeyCode.DOWN)) {
				handleArrowKey(e);
				return;
			}
			if(e.getCode().equals(KeyCode.DELETE) || e.getCode().equals(KeyCode.BACK_SPACE)) {
				handleRemove(e);
				return;
			}
			
			if(e.getText().length() == 1 && !".".equals(e.getText())) {
				//handle Insert Char
				filterSuggestions(e.getText(), CodeCompletionAction.INSERTION);
			}
		});
	}
	
	private void handleArrowKey(KeyEvent e) {
		boolean needReturn;
		if(e.getCode().equals(KeyCode.LEFT)) {
			needReturn = handleLeft();
			if(needReturn) {
				return;
			}
		} else if(e.getCode().equals(KeyCode.RIGHT)) {
			needReturn = handleRight();
			if(needReturn) {
				return;
			}
		} else if(e.getCode().equals(KeyCode.UP)) {
			handleUp(e);
			return;
		} else if(e.getCode().equals(KeyCode.DOWN)) {
			handleDown(e);
			return;
		}
		filterSuggestions("", CodeCompletionAction.ARROWKEY);
	}
	
	private boolean handleLeft() {
		if(getParent().getCurrentPosInLine() == 0 || ';' == getParent().getCurrentLine().charAt(getParent().getCurrentPosInLine() - 1) || '.' == getParent().getCurrentLine().charAt(getParent().getCurrentPosInLine() - 1)) {
			deactivate();
			return true;
		}
		currentPosInSuggestion = Math.max(0, currentPosInSuggestion-1);
		return false;
	}
	
	private boolean handleRight() {
		if(getParent().getCurrentPosInLine() == getParent().getCurrentLine().length() || ';' == getParent().getCurrentLine().charAt(getParent().getCurrentPosInLine())) {
			deactivate();
			return true;
		}
		if(getParent().getCaretPosition() != getParent().getText().length()) {
			currentSuggestion += getParent().getText().charAt(getParent().getCaretPosition());
			charCounterInSuggestion += 1;
			currentPosInSuggestion = Math.min(currentSuggestion.length(), currentPosInSuggestion + 1);
		}
		return false;
	}
	
	private void handleUp(KeyEvent e) {
		if(lvSuggestions.getSelectionModel().getSelectedIndex() == 0) {
			lvSuggestions.getSelectionModel().selectLast();
			lvSuggestions.scrollTo(suggestions.size()-1);
			e.consume();
		}
	}
	
	private void handleDown(KeyEvent e) {
		if(lvSuggestions.getSelectionModel().getSelectedIndex() == suggestions.size() - 1) {
			lvSuggestions.getSelectionModel().selectFirst();
			lvSuggestions.scrollTo(0);
			e.consume();
		}
	}
	
	private void handleRemove(KeyEvent e) {
		if(e.getCode().equals(KeyCode.DELETE)) {
			handleDeletion();
		} else if(e.getCode().equals(KeyCode.BACK_SPACE)) {
			handleBackspace();
		}
		filterSuggestions("", CodeCompletionAction.DELETION);
	}
	
	private void handleDeletion() {
		if(currentPosInSuggestion != charCounterInSuggestion) {
			charCounterInSuggestion--;
			currentSuggestion = currentSuggestion.substring(0, currentPosInSuggestion) + currentSuggestion.substring(Math.min(currentPosInSuggestion + 1, currentSuggestion.length()), currentSuggestion.length());
		}
		if(getParent().getCaretPosition() == getParent().getText().length()) {
			deactivate();
		}
	}
	
	private void handleBackspace() {
		if(currentPosInSuggestion != 0) {
			currentPosInSuggestion--;
			charCounterInSuggestion--;
			currentSuggestion = currentSuggestion.substring(0, currentPosInSuggestion) + currentSuggestion.substring(Math.max(currentPosInSuggestion + 1, currentSuggestion.length()), currentSuggestion.length());		
		}
		if(getParent().getCurrentPosInLine() == 0 || '.' == getParent().getCurrentLine().charAt(getParent().getCurrentPosInLine() - 1)) {
			deactivate();
		}
	}
	
	private void chooseMethod(Event e) {
		if(lvSuggestions.getSelectionModel().getSelectedItem() != null) {
			String choice = lvSuggestions.getSelectionModel().getSelectedItem().getNameAndParams();
			getParent().fireEvent(new CodeCompletionEvent(e, choice, choice.substring(0, currentPosInSuggestion)));
		}
		deactivate();
	}
	
	public GroovyConsole getParent() {
		return parent;
	}
	
	
	public boolean isVisible() {
		return this.isShowing();
	}
	
}
