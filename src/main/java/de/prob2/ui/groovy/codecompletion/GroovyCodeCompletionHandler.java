package de.prob2.ui.groovy.codecompletion;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;

import de.prob2.ui.groovy.GroovyConsole;
import de.prob2.ui.groovy.GroovyMethodOption;
import de.prob2.ui.groovy.MetaPropertiesHandler;
import de.prob2.ui.groovy.objects.GroovyAbstractItem;
import de.prob2.ui.groovy.objects.GroovyClassPropertyItem;
import de.prob2.ui.groovy.objects.GroovyObjectItem;
import javafx.collections.ObservableList;

public class GroovyCodeCompletionHandler {
	
	private final List<GroovyAbstractItem> currentSuggestions;
	
	private final ObservableList<GroovyAbstractItem> suggestions;
	
	public GroovyCodeCompletionHandler(ObservableList<GroovyAbstractItem> suggestions) {
		this.suggestions = suggestions;
		this.currentSuggestions = new ArrayList<GroovyAbstractItem>();
	}
	
	public void handleMethodsFromObjects(String currentLine, String currentSuggestion, TriggerAction action, GroovyConsole parent, ScriptEngine engine) {
		String[] methods = getMethodsFromCurrentLine(currentLine, action, parent);
		if(methods.length == 0) {
			return;
		}
		Object object = getObjectFromScope(methods[0], engine);
		if(object == null) {
			return;
		}
		Class<? extends Object> clazz = object.getClass();
		for(int i = 1; i < methods.length; i++) {
			fillAllMethodsAndProperties(clazz, GroovyMethodOption.NONSTATIC);
			for(GroovyAbstractItem item: currentSuggestions) {
				if(item.getNameAndParams().equals(methods[i])) {
						clazz = ((GroovyClassPropertyItem) item).getReturnTypeClass();
						break;
				}
				if(item.equals(currentSuggestions.get(currentSuggestions.size() - 1))) {
					return;
				}
			}
		}
		showSuggestions(clazz, GroovyMethodOption.NONSTATIC);
		if(action == TriggerAction.TRIGGER) {
			refresh(currentSuggestion);
		}
	}
	
	public void handleStaticClasses(String currentLine, String currentSuggestion, TriggerAction action, GroovyConsole parent) {
		String[] methods = getMethodsFromCurrentLine(currentLine, action, parent);
		Package[] packages = Package.getPackages();
		if(methods.length == 0) {
			return;
		}
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
		if(action == TriggerAction.TRIGGER) {
			refresh(currentSuggestion);
		}
	}
	
	public void handleObjects(TriggerAction action, ScriptEngine engine) {
		if(action == TriggerAction.TRIGGER) {
			currentSuggestions.clear();
			fillObjects(engine.getBindings(ScriptContext.ENGINE_SCOPE));
			fillObjects(engine.getBindings(ScriptContext.GLOBAL_SCOPE));
		}
	}
		
	private void fillAllMethodsAndProperties(Class <? extends Object> clazz, GroovyMethodOption option) {
		currentSuggestions.clear();
		fillMethodsAndProperties(clazz, option);
		MetaPropertiesHandler.handleMethods(clazz, currentSuggestions, option);
		MetaPropertiesHandler.handleProperties(clazz, currentSuggestions);
	}
	
	public void fillObjects(Bindings bindings) {
		suggestions.clear();
		for (final Map.Entry<String, Object> entry : bindings.entrySet()) {
			if(entry == null || entry.getKey() == null || entry.getValue() == null) {
				continue;
			}
			currentSuggestions.add(new GroovyObjectItem(entry.getKey(), entry.getValue(), null));
		}
		suggestions.addAll(currentSuggestions);
	}
		
	private void fillMethodsAndProperties(Class <? extends Object> clazz, GroovyMethodOption option) {
		for(Method m : clazz.getMethods()) {
			if((option == GroovyMethodOption.ALL) || (option == GroovyMethodOption.NONSTATIC && !Modifier.isStatic(m.getModifiers())) || (option == GroovyMethodOption.STATIC && Modifier.isStatic(m.getModifiers()))) {
				currentSuggestions.add(new GroovyClassPropertyItem(m));
			}
		}
		for(Field f : clazz.getFields()) {
			currentSuggestions.add(new GroovyClassPropertyItem(f));
		}
	}
	
	
	private void showSuggestions(Class <? extends Object> clazz, GroovyMethodOption option) {
		currentSuggestions.clear();
		suggestions.clear();
		fillMethodsAndProperties(clazz, option);
		MetaPropertiesHandler.handleMethods(clazz, currentSuggestions, option);
		MetaPropertiesHandler.handleProperties(clazz, currentSuggestions);
		suggestions.addAll(currentSuggestions);
	}
	
	
	public void refresh(String filter) {
		suggestions.clear();
		for(int i = 0; i < currentSuggestions.size(); i++) {
			GroovyAbstractItem suggestion = currentSuggestions.get(i);
			if(suggestion.getNameAndParams().toLowerCase().startsWith(filter.toLowerCase())) {
				suggestions.add(suggestion);
			}
		}
	}
	

	private Object getObjectFromScope(String currentLine, ScriptEngine engine) {
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
	
	
	private String[] getMethodsFromCurrentLine(String currentLine, TriggerAction action, GroovyConsole parent) {
		String currentInstruction = "";
		if(action == TriggerAction.POINT) {
			currentInstruction = currentLine.substring(0, parent.getCurrentPosInLine());
			if(parent.getCurrentPosInLine() == 0 || currentInstruction.charAt(parent.getCurrentPosInLine() - 1) == ';') {
				return new String[]{};
			}
		} else {
			currentInstruction = currentLine;
			if(!currentInstruction.contains(".")) {
				return new String[]{};
			}
		}
		currentInstruction = currentInstruction.replaceAll("\\s","");
		currentInstruction = currentInstruction.replaceAll("=", ";");
		currentInstruction = splitBraces(currentInstruction);
		String[] currentObjects = currentInstruction.split(";");
		return currentObjects[currentObjects.length-1].split("\\.");
	}
	
	private String splitBraces(String currentInstruction) {
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
	
	public void clear() {
		currentSuggestions.clear();
	}
	

}
