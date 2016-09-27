package de.prob2.ui.groovy;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import javafx.scene.shape.Path;
import javafx.stage.Popup;

public class GroovyCodeCompletion extends Popup {
	
	private final Logger logger = LoggerFactory.getLogger(GroovyCodeCompletion.class);
	
	@FXML
	private ListView<GroovyClassPropertyItem> lv_suggestions;
	
	private ObservableList<GroovyClassPropertyItem> suggestions = FXCollections.observableArrayList();
	
	private ScriptEngine engine;
		
	private GroovyConsole parent;
	
	private List<GroovyClassPropertyItem> currentObjectMethodsAndProperties;
	
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
		this.currentObjectMethodsAndProperties = new ArrayList<GroovyClassPropertyItem>();
		lv_suggestions.setItems(suggestions);
		lv_suggestions.setOnKeyPressed(e-> {
			
			if(e.getCode().equals(KeyCode.ENTER)) {
				if(lv_suggestions.getSelectionModel().getSelectedItem() != null) {
					getParent().fireEvent(new CodeCompletionEvent(e, lv_suggestions.getSelectionModel().getSelectedItem().getNameAndParams()));
				}
				deactivate();
			}
			
			if(e.getCode().equals(KeyCode.DELETE) || e.getCode().equals(KeyCode.BACK_SPACE)) {
				filterSuggestions("");
				if('.' == getParent().getCurrentLine().charAt(getParent().getCurrentLine().length() - 1)) {
					deactivate();
				}
				return;
				
			}
			if(e.getText().length() == 1 && !".".equals(e.getText())) {
				filterSuggestions(e.getText());
			}
		});
		
	}
	
	
	private void filterSuggestions(String addition) {
		String currentInstruction = getParent().getCurrentLine() + addition;
		if("".equals(addition)) {
			currentInstruction = currentInstruction.substring(0, currentInstruction.length()-1);
		}
		currentInstruction = getParent().getCurrentInstruction(currentInstruction);
		refresh(currentInstruction);
	}
	
	private void refresh(String filter) {
		suggestions.clear();
		for(int i = 0; i < currentObjectMethodsAndProperties.size(); i++) {
			GroovyClassPropertyItem suggestion = currentObjectMethodsAndProperties.get(i);
			if(suggestion.getNameAndParams().contains(filter)) {
				suggestions.add(suggestion);
			}
		}
		lv_suggestions.getSelectionModel().selectFirst();
		if(suggestions.isEmpty()) {
			this.deactivate();
		}
	}
	
	
	public void activate(GroovyConsole console, String currentLine) {
		this.parent = console;
		handleObjects(currentLine);
		showPopup(console);
	}
	
	private void handleObjects(String currentLine) {
		String currentObject = currentLine.substring(0, getParent().getCurrentPosInLine());	
		String[] methods = currentObject.split("\\.");
		Object object = getObjectFromScope(methods[0]);
		if(object == null) {
			return;
		}
		Class<? extends Object> clazz = object.getClass();
		for(int i = 1; i < methods.length; i++) {
			fillMethodsAndProperties(clazz);
			MetaPropertiesHandler.handleMethods(clazz, currentObjectMethodsAndProperties);
			MetaPropertiesHandler.handleProperties(clazz, currentObjectMethodsAndProperties);
			for(GroovyClassPropertyItem item: currentObjectMethodsAndProperties) {
				if(item.getNameAndParams().equals(methods[i])) {
						clazz = item.getReturnTypeClass();
						break;
				}
			}
		}
		showSuggestions(clazz);
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
		lv_suggestions.getSelectionModel().selectFirst();
		Point2D point = findCaretPosition(findCaret(console));
		double x = point.getX() + 10;
		double y = point.getY() + 20;
		this.show(console, x, y);	
	}
	
	public void deactivate() {
		suggestions.clear();
		currentObjectMethodsAndProperties.clear();
		this.hide();
	}
	
	private void fillMethodsAndProperties(Class <? extends Object> clazz) {
		for(Method m : clazz.getMethods()) {
			currentObjectMethodsAndProperties.add(new GroovyClassPropertyItem(m));
		}
		for(Field f : clazz.getFields()) {
			currentObjectMethodsAndProperties.add(new GroovyClassPropertyItem(f));
		}
	}
		
	private void showSuggestions(Class <? extends Object> clazz) {
		currentObjectMethodsAndProperties.clear();
		fillMethodsAndProperties(clazz);
		MetaPropertiesHandler.handleMethods(clazz, currentObjectMethodsAndProperties);
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
