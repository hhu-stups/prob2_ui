package de.prob2.ui.groovy.codecompletion;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.prob2.ui.groovy.GroovyConsole;
import de.prob2.ui.groovy.GroovyMethodOption;
import de.prob2.ui.groovy.MetaPropertiesHandler;
import de.prob2.ui.groovy.objects.GroovyClassPropertyItem;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.shape.Path;
import javafx.stage.Popup;

public class GroovyCodeCompletion extends Popup {
	
	private final Logger logger = LoggerFactory.getLogger(GroovyCodeCompletion.class);
	
	@FXML
	private ListView<GroovyClassPropertyItem> lvSuggestions;
	
	private ObservableList<GroovyClassPropertyItem> suggestions = FXCollections.observableArrayList();
	
	private ScriptEngine engine;
		
	private GroovyConsole parent;
	
	private List<GroovyClassPropertyItem> currentObjectMethodsAndProperties;
	
	private String currentSuggestion;
	
	private int currentPosInSuggestion;
	
	private int charCounterInSuggestion;
	
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
		this.currentObjectMethodsAndProperties = new ArrayList<>();
		this.currentSuggestion = "";
		this.currentPosInSuggestion = 0;
		this.charCounterInSuggestion = 0;
		lvSuggestions.setItems(suggestions);
		lvSuggestions.setOnMouseClicked(this::chooseMethod);
		lvSuggestions.setOnKeyPressed(e-> {
			
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
			if(e.getCode().equals(KeyCode.LEFT) || e.getCode().equals(KeyCode.RIGHT)) {
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
			if('.' == getParent().getCurrentLine().charAt(getParent().getCurrentPosInLine() - 1)) {
				deactivate();
				return;
			}
			currentPosInSuggestion = Math.max(0, currentPosInSuggestion-1);

		} else if(e.getCode().equals(KeyCode.RIGHT)) {
			if(';' == getParent().getCurrentLine().charAt(getParent().getCurrentPosInLine() - 1)) {
				deactivate();
				return;
			}
			currentPosInSuggestion = Math.min(currentSuggestion.length(), currentPosInSuggestion + 1);
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
			getParent().fireEvent(new CodeCompletionEvent(e, lvSuggestions.getSelectionModel().getSelectedItem().getNameAndParams(), currentSuggestion.substring(0, currentPosInSuggestion)));
		}
		deactivate();
	}
	
	private void chooseFirst(Event e) {
		if(lvSuggestions.getItems().get(0) != null) {
			getParent().fireEvent(new CodeCompletionEvent(e, lvSuggestions.getItems().get(0).getNameAndParams(), currentSuggestion.substring(0, currentPosInSuggestion)));
		}
		deactivate();
	}
	
	
	private void filterSuggestions(String addition, CodeCompletionAction action) {
		String currentInstruction = currentSuggestion;
		if(action.equals(CodeCompletionAction.ARROWKEY)) {
			currentInstruction = currentSuggestion.substring(0, currentPosInSuggestion);
		} else if(action.equals(CodeCompletionAction.INSERTION)) {
			currentSuggestion = new StringBuilder(currentSuggestion).insert(currentPosInSuggestion, addition.charAt(0)).toString();
			currentPosInSuggestion++;
			charCounterInSuggestion++;
			currentInstruction = currentSuggestion;
		}
		refresh(currentInstruction);
	}
	
	
	private void refresh(String filter) {
		suggestions.clear();
		for(int i = 0; i < currentObjectMethodsAndProperties.size(); i++) {
			GroovyClassPropertyItem suggestion = currentObjectMethodsAndProperties.get(i);
			if(suggestion.getNameAndParams().startsWith(filter)) {
				suggestions.add(suggestion);
			}
		}
		lvSuggestions.getSelectionModel().selectFirst();
		if(suggestions.isEmpty()) {
			this.deactivate();
		}
	}
	
	
	public void activate(GroovyConsole console, String currentLine) {
		this.parent = console;
		handleObjects(currentLine);
		handleStaticClasses(currentLine);
		showPopup(console);
	}
	
	private void handleStaticClasses(String currentLine) {
		String[] methods = getMethodsFromCurrentLine(currentLine);
		Package[] packages = Package.getPackages();
		for (Package pack : packages) {
		    String fullClassName= pack.getName() + "." + methods[methods.length - 1];
		    Class <? extends Object> clazz = null;
		    try {
		        clazz = Class.forName(fullClassName);
	    		fillAllMethodsAndProperties(clazz, GroovyMethodOption.STATIC);
				showSuggestions(clazz, GroovyMethodOption.STATIC);
		    } catch (ClassNotFoundException ignored) { //NOSONAR
		        // Just try with the next package if the current fullClassName does not fit any classes
		    }
		}
	}
	
	private void handleObjects(String currentLine) {
		String[] methods = getMethodsFromCurrentLine(currentLine);
		if(methods == null) {
			return;
		}
		Object object = getObjectFromScope(methods[0]);
		if(object == null) {
			return;
		}
		Class<? extends Object> clazz = object.getClass();
		for(int i = 1; i < methods.length; i++) {
			fillAllMethodsAndProperties(clazz, GroovyMethodOption.NONSTATIC);
			for(GroovyClassPropertyItem item: currentObjectMethodsAndProperties) {
				if(item.getNameAndParams().equals(methods[i])) {
						clazz = item.getReturnTypeClass();
						break;
				}
				if(item.equals(currentObjectMethodsAndProperties.get(currentObjectMethodsAndProperties.size() - 1))) {
					return;
				}
			}
		}
		showSuggestions(clazz, GroovyMethodOption.NONSTATIC);
	}
	
	private String[] getMethodsFromCurrentLine(String currentLine) {
		String currentInstruction = currentLine.substring(0, getParent().getCurrentPosInLine());	
		if(getParent().getCurrentPosInLine() == 0 || currentInstruction.charAt(getParent().getCurrentPosInLine() - 1) == ';') {
			return null;
		}
		currentInstruction = currentInstruction.replaceAll("\\s","");
		currentInstruction = splitBraces(currentInstruction);
		String[] currentObjects = currentInstruction.split(";");
		String[] methods = currentObjects[currentObjects.length-1].split("\\.");
		return methods;
	}
	
	private void fillAllMethodsAndProperties(Class <? extends Object> clazz, GroovyMethodOption option) {
		fillMethodsAndProperties(clazz, option);
		MetaPropertiesHandler.handleMethods(clazz, currentObjectMethodsAndProperties, option);
		MetaPropertiesHandler.handleProperties(clazz, currentObjectMethodsAndProperties);
	}
	
	public String splitBraces(String currentInstruction) {
		char[] instruction = currentInstruction.toCharArray();
		String result = "";
		for(int i = 0; i < instruction.length; i++) {
			if(instruction[i] == '(' && instruction[i+1] != ')') {
				result = new StringBuilder(result).append(";").toString();
			} else {
				result = new StringBuilder(result).append(instruction[i]).toString();
			}
		}
		return result;
	}
			
	private Object getObjectFromScope(String currentLine) {
		Bindings engineScope = engine.getBindings(ScriptContext.ENGINE_SCOPE);
		Bindings globalScope = engine.getBindings(ScriptContext.GLOBAL_SCOPE);
		Object object = null;
		if(engineScope.keySet().contains(currentLine)) {
			object = engineScope.get(currentLine);
		} else if(globalScope.keySet().contains(currentLine)) {
			object = globalScope.get(currentLine);
		}
		return object;
	}
	
	public GroovyConsole getParent() {
		return parent;
	}
	
	private void showPopup(GroovyConsole console) {
		if(suggestions.isEmpty()) {
			return;
		}
		lvSuggestions.getSelectionModel().selectFirst();
		Point2D point = findCaretPosition(findCaret(console));
		double x = point.getX() + 10;
		double y = point.getY() + 20;
		this.show(console, x, y);	
	}
	
	public void deactivate() {
		suggestions.clear();
		currentObjectMethodsAndProperties.clear();
		currentSuggestion ="";
		currentPosInSuggestion = 0;
		charCounterInSuggestion = 0;
		this.hide();
	}
	
	private void fillMethodsAndProperties(Class <? extends Object> clazz, GroovyMethodOption option) {
		for(Method m : clazz.getMethods()) {
			if((option == GroovyMethodOption.ALL) || (option == GroovyMethodOption.NONSTATIC && !Modifier.isStatic(m.getModifiers())) || (option == GroovyMethodOption.STATIC && Modifier.isStatic(m.getModifiers()))) {
				currentObjectMethodsAndProperties.add(new GroovyClassPropertyItem(m));
			}
		}
		for(Field f : clazz.getFields()) {
			currentObjectMethodsAndProperties.add(new GroovyClassPropertyItem(f));
		}
	}
		
	private void showSuggestions(Class <? extends Object> clazz, GroovyMethodOption option) {
		currentObjectMethodsAndProperties.clear();
		fillMethodsAndProperties(clazz, option);
		MetaPropertiesHandler.handleMethods(clazz, currentObjectMethodsAndProperties, option);
		MetaPropertiesHandler.handleProperties(clazz, currentObjectMethodsAndProperties);
		suggestions.addAll(currentObjectMethodsAndProperties);
	}
	
	public boolean isVisible() {
		return this.isShowing();
	}
	
	
	private Path findCaret(Parent parent) {
		for (Node node : parent.getChildrenUnmodifiable()) {
			if (node instanceof Path) {
				return (Path) node;
			} else if (node instanceof Parent) {
				Path caret = findCaret((Parent) node);
				if (caret != null) {
					return caret;
				}
			}
		}
		return null;
	}
	

	private Point2D findCaretPosition(Node node) {
		double x = 0;
		double y = 0;
		if(node == null) {
			return null;
		}
		for (Node n = node; n != null; n=n.getParent()) {
			Bounds parentBounds = n.getBoundsInParent();
			x += parentBounds.getMinX();
			y += parentBounds.getMinY();
		}
		if(node.getScene() != null) {
			Scene scene = node.getScene();
			x += scene.getX() + scene.getWindow().getX();
			y += scene.getY() + scene.getWindow().getY();
			x = Math.min(scene.getWindow().getX() + scene.getWindow().getWidth() - 20, x);
			y = Math.min(scene.getWindow().getY() + scene.getWindow().getHeight() - 20, y);
		}
		return new Point2D(x,y);
	}

}
