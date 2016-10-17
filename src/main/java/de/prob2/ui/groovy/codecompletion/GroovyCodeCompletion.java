package de.prob2.ui.groovy.codecompletion;

import java.io.IOException;
import javax.script.ScriptEngine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.prob2.ui.groovy.GroovyConsole;
import de.prob2.ui.groovy.objects.GroovyAbstractItem;
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
			currentLine = currentLine.replaceAll("\\s","");
			int indexOfPoint = currentLine.lastIndexOf('.');
			int indexOfSemicolon = currentLine.lastIndexOf(";");
			if(indexOfPoint != -1) {
				if(indexOfSemicolon <= getParent().getCurrentPosInLine()) {
					currentSuggestion = currentLine.substring(indexOfSemicolon + 1, currentLine.length());
					currentPosInSuggestion = currentSuggestion.length();
					charCounterInSuggestion = currentPosInSuggestion;
					currentPrefix = "";
				} else {
					currentSuggestion = currentLine.substring(indexOfPoint + 1, currentLine.length());
					currentPosInSuggestion = currentSuggestion.length();
					charCounterInSuggestion = currentPosInSuggestion;
					currentPrefix = currentLine.substring(0, indexOfPoint + 1);
				} 
			}
		}
		completionHandler.handleMethodsFromObjects(currentPrefix, currentSuggestion, action, parent, engine);
		completionHandler.handleStaticClasses(currentPrefix, currentSuggestion, action, parent);
		if(suggestions.isEmpty()) {
			completionHandler.handleObjects(action, engine);
		}
		showPopup(console);
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
		suggestions.sort((o1,o2) -> {
			return o1.toString().compareToIgnoreCase(o2.toString());
		});
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
			currentInstruction = currentSuggestion.substring(0, currentPosInSuggestion);
		} else if(action.equals(CodeCompletionAction.INSERTION)) {
			currentSuggestion = new StringBuilder(currentSuggestion).insert(currentPosInSuggestion, addition.charAt(0)).toString();
			currentPosInSuggestion++;
			charCounterInSuggestion++;
			currentInstruction = currentSuggestion;
		}
		completionHandler.refresh(currentInstruction);
		sortSuggestions();
		lvSuggestions.getSelectionModel().selectFirst();
		if(suggestions.isEmpty()) {
			this.deactivate();
		}
	}
	

	private void setListeners() {
		lvSuggestions.setOnMouseClicked(this::chooseMethod);
		lvSuggestions.setOnKeyPressed(e-> {
			if(e.getCode().equals(KeyCode.SPACE)) {
				getParent().fireEvent(new CodeCompletionEvent(e));
			}
			if(e.getCode().equals(KeyCode.ENTER)) {
				//handle Enter in Groovy Code Completion
				chooseMethod(e);
				return;
			}
			if(";".equals(e.getText())) {
				//handle Semicolon
				chooseFirst(e);
				return;
			}
			if(e.getCode().equals(KeyCode.LEFT) || e.getCode().equals(KeyCode.RIGHT) || e.getCode().equals(KeyCode.UP) || e.getCode().equals(KeyCode.DOWN)) {
				handleArrowKey(e);
				return;
			}
			
			
			if(e.getCode().equals(KeyCode.DELETE) || e.getCode().equals(KeyCode.BACK_SPACE)) {
				handleDeletion(e);
				return;
			}
			
			if(e.getText().length() == 1 && !".".equals(e.getText())) {
				//handle Insert Char
				filterSuggestions(e.getText(), CodeCompletionAction.INSERTION);
			}
		});
	}
	
	private void handleArrowKey(KeyEvent e) {
		if(e.getCode().equals(KeyCode.LEFT)) {
			if(getParent().getCurrentPosInLine() == 0 || ';' == getParent().getCurrentLine().charAt(getParent().getCurrentPosInLine() - 1) || '.' == getParent().getCurrentLine().charAt(getParent().getCurrentPosInLine() - 1)) {
				deactivate();
				return;
			}
			currentPosInSuggestion = Math.max(0, currentPosInSuggestion-1);

		} else if(e.getCode().equals(KeyCode.RIGHT)) {
			if(getParent().getCurrentPosInLine() == getParent().getCurrentLine().length() || ';' == getParent().getCurrentLine().charAt(getParent().getCurrentPosInLine())) {
				deactivate();
				return;
			}
			if(getParent().getCaretPosition() != getParent().getText().length()) {
				currentSuggestion += getParent().getText().charAt(getParent().getCaretPosition());
				charCounterInSuggestion += 1;
				currentPosInSuggestion = Math.min(currentSuggestion.length(), currentPosInSuggestion + 1);
			}
		} else if(e.getCode().equals(KeyCode.UP)) {
			if(lvSuggestions.getSelectionModel().getSelectedIndex() == 0) {
				lvSuggestions.getSelectionModel().selectLast();
				lvSuggestions.scrollTo(suggestions.size()-1);
				e.consume();
			}
			return;
		} else if(e.getCode().equals(KeyCode.DOWN)) {
			if(lvSuggestions.getSelectionModel().getSelectedIndex() == suggestions.size() - 1) {
				lvSuggestions.getSelectionModel().selectFirst();
				lvSuggestions.scrollTo(0);
				e.consume();
			}
			return;
		}
		filterSuggestions("", CodeCompletionAction.ARROWKEY);
	}
	
	private void handleDeletion(KeyEvent e) {
		if(e.getCode().equals(KeyCode.DELETE) && currentPosInSuggestion != charCounterInSuggestion) {
			charCounterInSuggestion--;
			currentSuggestion = currentSuggestion.substring(0, currentPosInSuggestion) + currentSuggestion.substring(Math.min(currentPosInSuggestion + 1, currentSuggestion.length()), currentSuggestion.length());
		} else if(e.getCode().equals(KeyCode.BACK_SPACE) && currentPosInSuggestion != 0) {
			currentPosInSuggestion--;
			charCounterInSuggestion--;
			currentSuggestion = currentSuggestion.substring(0, currentPosInSuggestion) + currentSuggestion.substring(Math.max(currentPosInSuggestion + 1, currentSuggestion.length()), currentSuggestion.length());				
		}
		filterSuggestions("", CodeCompletionAction.DELETION);
		if('.' == getParent().getCurrentLine().charAt(getParent().getCurrentPosInLine() - 1)) {
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
	
	private void chooseFirst(Event e) {
		if(lvSuggestions.getItems().get(0) != null) {
			String choice = lvSuggestions.getItems().get(0).getNameAndParams();
			getParent().fireEvent(new CodeCompletionEvent(e, lvSuggestions.getItems().get(0).getNameAndParams(), choice.substring(0, currentPosInSuggestion)));
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
