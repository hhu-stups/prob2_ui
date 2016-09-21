package de.prob2.ui.groovy;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;

import com.google.inject.Inject;

import de.prob.scripting.ScriptEngineProvider;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import javafx.scene.shape.Path;
import javafx.stage.Popup;

public class GroovyCodeCompletion {
	
	private Popup popup;
	
	private ListView<Object> lv_suggestions;
	
	private final ScriptEngine engine;
	
	private final MetaPropertiesHandler groovyHandler;

	
	//Trying with ListView
	
	@Inject
	public GroovyCodeCompletion(final ScriptEngineProvider sep, final MetaPropertiesHandler groovyHandler) {
		engine = sep.get();
		popup = new Popup();
		this.groovyHandler = groovyHandler;
		lv_suggestions = new ListView<Object>();
		lv_suggestions.setOnKeyPressed(e-> {
			if(e.getCode().equals(KeyCode.ENTER)) {
				System.out.println(lv_suggestions.getSelectionModel().getSelectedItem());
				this.deactivate();
			}
		});
		lv_suggestions.setMaxHeight(200);
		lv_suggestions.setMaxWidth(400);
		lv_suggestions.setPrefHeight(200);
		lv_suggestions.setPrefWidth(400);
		popup.getContent().add(lv_suggestions);
	}
	
	
	public void activate(GroovyConsole console, String currentLine) {
		Bindings engineScope = engine.getBindings(ScriptContext.ENGINE_SCOPE);
		Bindings globalScope = engine.getBindings(ScriptContext.GLOBAL_SCOPE);
		if(!engineScope.keySet().contains(currentLine) && !globalScope.keySet().contains(currentLine)) {
			return;
		}
		Object object = null;
		if(engineScope.keySet().contains(currentLine)) {
			object = engineScope.get(currentLine);
		} else if(globalScope.keySet().contains(currentLine)) {
			object = globalScope.get(currentLine);
		} else {
			return;
		}
		showSuggestions(object);
		lv_suggestions.getSelectionModel().selectFirst();
		Point2D point = findCaretPosition(findCaret(console));
		double x = point.getX() + 10;
		double y = point.getY() + 10;
		popup.show(console, x, y);
		
	}
	
	
	public void deactivate() {
		popup.hide();
	}
	
	private void showSuggestions(Object object) {
		Class <? extends Object> clazz = object.getClass();
		for(Method m : clazz.getMethods()) {
			lv_suggestions.getItems().add(m.getName());
		}
		for(Field f : clazz.getFields()) {
			lv_suggestions.getItems().add(f.getName());
		}
		groovyHandler.handleMethods(object, lv_suggestions.getItems());
		groovyHandler.handleProperties(object, lv_suggestions.getItems());
		
	}
	
	public boolean isVisible() {
		return popup.isShowing();
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
