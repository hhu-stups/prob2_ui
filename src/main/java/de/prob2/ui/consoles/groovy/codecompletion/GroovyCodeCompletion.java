package de.prob2.ui.consoles.groovy.codecompletion;

import java.util.Optional;

import javax.script.ScriptEngine;

import de.prob2.ui.consoles.groovy.GroovyConsole;
import de.prob2.ui.consoles.groovy.objects.GroovyAbstractItem;
import de.prob2.ui.internal.StageManager;

import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Popup;

public class GroovyCodeCompletion extends Popup {
	private final ObservableList<GroovyAbstractItem> suggestions;
	private final ScriptEngine engine;
	private final GroovyCodeCompletionHandler completionHandler;
	@FXML
	private ListView<GroovyAbstractItem> lvSuggestions;
	private GroovyConsole parent;
	private String currentSuggestion;
	private int currentPosInSuggestion;
	private int charCounterInSuggestion;

	public GroovyCodeCompletion(StageManager stageManager, ScriptEngine engine) {
		this.engine = engine;
		this.parent = null;
		this.currentSuggestion = "";
		this.currentPosInSuggestion = 0;
		this.charCounterInSuggestion = 0;
		this.suggestions = FXCollections.observableArrayList();
		this.completionHandler = new GroovyCodeCompletionHandler(suggestions);
		stageManager.loadFXML(this, "groovy_codecompletion_popup.fxml");
	}

	@FXML
	public void initialize() {
		lvSuggestions.setItems(suggestions);
		setListeners();
	}

	public void activate(String currentLine, CodeCompletionTriggerAction action) {
		String newCurrentLine = currentLine;
		if (action == CodeCompletionTriggerAction.POINT) {
			newCurrentLine += ".";
			if (currentLine.endsWith(".")) {
				return;
			}
		}
		String currentPrefix = handleActivation(newCurrentLine);
		completionHandler.handleMethodsFromObjects(currentPrefix, currentSuggestion, action, engine);
		completionHandler.handleStaticClasses(currentPrefix, currentSuggestion, action);
		completionHandler.handleObjects(currentSuggestion, action, engine);
		showPopup();
	}

	private String handleActivation(String currentLine) {
		String currentPrefix;
		String newCurrentLine = currentLine.replace("\\s", "");
		int indexOfPoint = newCurrentLine.lastIndexOf('.');
		int index = Math.max(-1, indexOfPoint);
		currentSuggestion = newCurrentLine.substring(index + 1);
		currentPosInSuggestion = currentSuggestion.length();
		charCounterInSuggestion = currentPosInSuggestion;
		currentPrefix = newCurrentLine.substring(0, index + 1);
		return currentPrefix;
	}

	private void showPopup() {
		if (suggestions.isEmpty()) {
			return;
		}
		sortSuggestions();
		lvSuggestions.getSelectionModel().selectFirst();
		this.show(parent.getScene().getWindow());
	}

	private void sortSuggestions() {
		suggestions.sort((o1, o2) -> o1.toString().compareToIgnoreCase(o2.toString()));
	}

	public void deactivate() {
		suggestions.clear();
		completionHandler.clear();
		currentSuggestion = "";
		currentPosInSuggestion = 0;
		charCounterInSuggestion = 0;
		this.hide();
	}

	public void filterSuggestions(String addition, CodeCompletionAction action) {
		String currentInstruction = currentSuggestion;
		if (action.equals(CodeCompletionAction.ARROWKEY)) {
			//handle Arrow Key
			currentInstruction = currentSuggestion.substring(0, currentPosInSuggestion);
		} else if (action.equals(CodeCompletionAction.INSERTION)) {
			currentInstruction = handleInsertChar(addition);
		}
		completionHandler.refresh(currentInstruction);
		sortSuggestions();
		lvSuggestions.getSelectionModel().selectFirst();
		if (suggestions.isEmpty()) {
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
		lvSuggestions.setOnKeyPressed(this::keyPressed);
	}

	private void keyPressed(KeyEvent e) {
		if (e.getCode().equals(KeyCode.SPACE)) {
			getParent().fireEvent(new CodeCompletionEvent(e));
		}
		if (";".equals(e.getText()) || e.getCode().equals(KeyCode.ENTER)) {
			//handle Enter in Groovy Code Completion
			chooseMethod(e);
			return;
		}
		if (e.getCode().equals(KeyCode.LEFT) || e.getCode().equals(KeyCode.RIGHT) || e.getCode().equals(KeyCode.UP) || e.getCode().equals(KeyCode.DOWN)) {
			handleArrowKey(e);
			return;
		}
		if (e.getCode().equals(KeyCode.DELETE) || e.getCode().equals(KeyCode.BACK_SPACE)) {
			handleRemove(e);
			return;
		}

		if (e.getText().length() == 1 && !".".equals(e.getText())) {
			//handle Insert Char
			filterSuggestions(e.getText(), CodeCompletionAction.INSERTION);
		}

		if (".".equals(e.getText())) {
			deactivate();
		}

	}

	private void handleArrowKey(KeyEvent e) {
		boolean needReturn;
		if (e.getCode().equals(KeyCode.LEFT)) {
			needReturn = handleLeft();
			if (needReturn) {
				return;
			}
		} else if (e.getCode().equals(KeyCode.RIGHT)) {
			needReturn = handleRight();
			if (needReturn) {
				return;
			}
		} else if (e.getCode().equals(KeyCode.UP)) {
			handleUp(e);
			return;
		} else if (e.getCode().equals(KeyCode.DOWN)) {
			handleDown(e);
			return;
		}
		filterSuggestions("", CodeCompletionAction.ARROWKEY);
	}

	private boolean handleLeft() {
		/*if (getParent().getCurrentPosInLine() == 0 || getParent().getInput().charAt(getParent().getCurrentPosInLine() - 1) == ';' || getParent().getInput().charAt(getParent().getCurrentPosInLine() - 1) == '.') {
			deactivate();
			return true;
		}
		currentPosInSuggestion = Math.max(0, currentPosInSuggestion - 1);*/
		return false;
	}

	private boolean handleRight() {
		/*if (getParent().getCurrentPosInLine() == getParent().getInput().length() || getParent().getInput().charAt(getParent().getCurrentPosInLine()) == ';') {
			deactivate();
			return true;
		}
		if (getParent().getCaretPosition() != getParent().getLength()) {
			currentSuggestion += getParent().getText(getParent().getCaretPosition(), getParent().getCaretPosition() + 1);
			charCounterInSuggestion += 1;
			currentPosInSuggestion = Math.min(currentSuggestion.length(), currentPosInSuggestion + 1);
		}*/
		return false;
	}

	private void handleUp(KeyEvent e) {
		if (lvSuggestions.getSelectionModel().getSelectedIndex() == 0) {
			lvSuggestions.getSelectionModel().selectLast();
			lvSuggestions.scrollTo(suggestions.size() - 1);
			e.consume();
		}
	}

	private void handleDown(KeyEvent e) {
		if (lvSuggestions.getSelectionModel().getSelectedIndex() == suggestions.size() - 1) {
			lvSuggestions.getSelectionModel().selectFirst();
			lvSuggestions.scrollTo(0);
			e.consume();
		}
	}

	private void handleRemove(KeyEvent e) {
		if (e.getCode().equals(KeyCode.DELETE)) {
			handleDeletion();
		} else if (e.getCode().equals(KeyCode.BACK_SPACE)) {
			handleBackspace();
		}
		filterSuggestions("", CodeCompletionAction.DELETION);
	}

	private void handleDeletion() {
		/*if (currentPosInSuggestion != charCounterInSuggestion) {
			charCounterInSuggestion--;
			currentSuggestion = currentSuggestion.substring(0, currentPosInSuggestion) + currentSuggestion.substring(Math.min(currentPosInSuggestion + 1, currentSuggestion.length()), currentSuggestion.length());
		}
		if (getParent().getCaretPosition() == getParent().getLength()) {
			deactivate();
		}*/
	}

	private void handleBackspace() {
		/*if (currentPosInSuggestion != 0) {
			currentPosInSuggestion--;
			charCounterInSuggestion--;
			currentSuggestion = currentSuggestion.substring(0, currentPosInSuggestion) + currentSuggestion.substring(Math.max(currentPosInSuggestion + 1, currentSuggestion.length()), currentSuggestion.length());
		}
		if (getParent().getCurrentPosInLine() == 0 || getParent().getInput().charAt(getParent().getCurrentPosInLine() - 1) == '.') {
			deactivate();
		}*/
	}

	private void chooseMethod(Event e) {
		if (lvSuggestions.getSelectionModel().getSelectedItem() != null) {
			String choice = lvSuggestions.getSelectionModel().getSelectedItem().getNameAndParams();
			getParent().fireEvent(new CodeCompletionEvent(e, choice, choice.substring(0, currentPosInSuggestion)));
		}
		deactivate();
	}

	public GroovyConsole getParent() {
		return parent;
	}

	public void setParent(GroovyConsole parent) {
		this.parent = parent;
		final ChangeListener<Optional<Bounds>> listener = (observable, from, to) -> to.ifPresent(bounds -> {
			this.setAnchorX((bounds.getMinX() + bounds.getMaxX()) / 2.0);
			this.setAnchorY(bounds.getMaxY());
		});
		parent.caretBoundsProperty().addListener(listener);
		listener.changed(parent.caretBoundsProperty(), Optional.empty(), parent.getCaretBounds());
	}

	public boolean isVisible() {
		return this.isShowing();
	}
}
