package de.prob2.ui.groovy;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	
	private ScriptEngine engine;
		
	private GroovyConsole parent;
	
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
		lv_suggestions.setOnKeyPressed(e-> {
			if(e.getCode().equals(KeyCode.ENTER)) {
				getParent().fireEvent(new CodeCompletionEvent(e, lv_suggestions.getSelectionModel().getSelectedItem().getNameAndParams()));
				deactivate();
			}
		});
		
	}
	
	
	public void activate(GroovyConsole console, String currentLine) {
		this.parent = console;
		Bindings engineScope = engine.getBindings(ScriptContext.ENGINE_SCOPE);
		Bindings globalScope = engine.getBindings(ScriptContext.GLOBAL_SCOPE);
		if(!engineScope.keySet().contains(currentLine) && !globalScope.keySet().contains(currentLine)) {
			return;
		}
		Object object;
		if(engineScope.keySet().contains(currentLine)) {
			object = engineScope.get(currentLine);
		} else if(globalScope.keySet().contains(currentLine)) {
			object = globalScope.get(currentLine);
		} else {
			return;
		}
		if(object == null) {
			return;
		}
		showSuggestions(object);
		showPopup(console);
	}
	
	public GroovyConsole getParent() {
		return parent;
	}
	
	private void showPopup(GroovyConsole console) {
		lv_suggestions.getSelectionModel().selectFirst();
		Point2D point = findCaretPosition(findCaret(console));
		double x = point.getX() + 10;
		double y = point.getY() + 20;
		this.show(console, x, y);	
	}
	
	public void deactivate() {
		lv_suggestions.getItems().clear();
		this.hide();
	}
	
	private void showSuggestions(Object object) {
		Class <? extends Object> clazz = object.getClass();
		for(Method m : clazz.getMethods()) {
			lv_suggestions.getItems().add(new GroovyClassPropertyItem(m));
		}
		for(Field f : clazz.getFields()) {
			lv_suggestions.getItems().add(new GroovyClassPropertyItem(f));
		}
		MetaPropertiesHandler.handleMethods(object, lv_suggestions.getItems());
		MetaPropertiesHandler.handleProperties(object, lv_suggestions.getItems());
		
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
