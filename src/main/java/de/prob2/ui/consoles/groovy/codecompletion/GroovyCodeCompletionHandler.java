package de.prob2.ui.consoles.groovy.codecompletion;

import de.prob2.ui.consoles.groovy.GroovyMethodOption;
import de.prob2.ui.consoles.groovy.objects.GroovyAbstractItem;
import de.prob2.ui.consoles.groovy.objects.GroovyClassHandler;
import de.prob2.ui.consoles.groovy.objects.GroovyClassPropertyItem;
import de.prob2.ui.consoles.groovy.objects.GroovyObjectItem;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GroovyCodeCompletionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GroovyCodeCompletionHandler.class);

    private final List<GroovyAbstractItem> currentSuggestions;

    private final ObservableList<GroovyAbstractItem> suggestions;

    public GroovyCodeCompletionHandler(ObservableList<GroovyAbstractItem> suggestions) {
        this.suggestions = suggestions;
        this.currentSuggestions = new ArrayList<>();
    }

    public void handleMethodsFromObjects(String currentLine, String currentSuggestion, CodeCompletionTriggerAction action, ScriptEngine engine) {
        String[] methods = getMethodsFromCurrentLine(currentLine);
        if (methods.length == 0) {
            return;
        }
        Object object = getObjectFromScope(methods[0], engine);
        if (object == null) {
            return;
        }
        Class<?> clazz = object.getClass();
        for (int i = 1; i < methods.length; i++) {
            fillAllMethodsAndProperties(clazz, GroovyMethodOption.NONSTATIC);
            for (GroovyAbstractItem item : currentSuggestions) {
                if (item.getNameAndParams().equals(methods[i])) {
                    clazz = ((GroovyClassPropertyItem) item).getReturnTypeClass();
                    break;
                }
                if (item.equals(currentSuggestions.get(currentSuggestions.size() - 1))) {
                    return;
                }
            }
        }
        showSuggestions(clazz, GroovyMethodOption.NONSTATIC);
        if (action == CodeCompletionTriggerAction.TRIGGER) {
            refresh(currentSuggestion);
        }
    }

    public void handleStaticClasses(String currentLine, String currentSuggestion, CodeCompletionTriggerAction action) {
        final String[] methods = getMethodsFromCurrentLine(currentLine);
        if (methods.length == 0) {
            return;
        }
        final StringBuilder classNameBuilder = new StringBuilder(methods[0]);
        for (int i = 1; i <= methods.length; i++) {
            // For each possible prefix of the "methods" array, try to find a class with that name.
            // For example, when the user types "java.lang.Boolean.FALSE.", this tries to find classes called java, java.lang, java.lang.Boolean, and java.lang.Boolean.FALSE (and stops at the first valid class name, java.lang.Boolean).
            try {
                final Class<?> clazz = Class.forName(classNameBuilder.toString());
                fillAllMethodsAndProperties(clazz, GroovyMethodOption.STATIC);
                showSuggestions(clazz, GroovyMethodOption.STATIC);
                break;
            } catch (ClassNotFoundException e) {
                LOGGER.trace("{} is not a class name (this is not an error)", e.getMessage());
            }
            // Special case for the last iteration
            if (i < methods.length) {
                classNameBuilder.append('.');
                classNameBuilder.append(methods[i]);
            }
        }
        if (action == CodeCompletionTriggerAction.TRIGGER) {
            refresh(currentSuggestion);
        }
    }

    public void handleObjects(String currentSuggestion, CodeCompletionTriggerAction action, ScriptEngine engine) {
        if (action == CodeCompletionTriggerAction.TRIGGER && suggestions.isEmpty()) {
            currentSuggestions.clear();
            fillObjects(engine.getBindings(ScriptContext.ENGINE_SCOPE));
            fillObjects(engine.getBindings(ScriptContext.GLOBAL_SCOPE));
            refresh(currentSuggestion);
        }
    }

    private void fillAllMethodsAndProperties(Class<?> clazz, GroovyMethodOption option) {
        currentSuggestions.clear();
        fillMethodsAndProperties(clazz, option);
        GroovyClassHandler.handleMethods(clazz, currentSuggestions, option);
        GroovyClassHandler.handleProperties(clazz, currentSuggestions);
    }

    public void fillObjects(Bindings bindings) {
        suggestions.clear();
        for (final Map.Entry<String, Object> entry : bindings.entrySet()) {
            if (entry == null || entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            currentSuggestions.add(new GroovyObjectItem(entry.getKey(), entry.getValue(), null));
        }
        suggestions.addAll(currentSuggestions);
    }

    private void fillMethodsAndProperties(Class<?> clazz, GroovyMethodOption option) {
        for (Method m : clazz.getMethods()) {
            if ((option == GroovyMethodOption.ALL) || isNonstatic(option, m) || isStatic(option, m)) {
                currentSuggestions.add(new GroovyClassPropertyItem(m));
            }
        }
        for (Field f : clazz.getFields()) {
            currentSuggestions.add(new GroovyClassPropertyItem(f));
        }
    }

    private boolean isNonstatic(GroovyMethodOption option, Method m) {
        return option == GroovyMethodOption.NONSTATIC && !Modifier.isStatic(m.getModifiers());
    }

    private boolean isStatic(GroovyMethodOption option, Method m) {
        return option == GroovyMethodOption.STATIC && Modifier.isStatic(m.getModifiers());
    }

    private void showSuggestions(Class<?> clazz, GroovyMethodOption option) {
        currentSuggestions.clear();
        suggestions.clear();
        fillMethodsAndProperties(clazz, option);
        GroovyClassHandler.handleMethods(clazz, currentSuggestions, option);
        GroovyClassHandler.handleProperties(clazz, currentSuggestions);
        suggestions.addAll(currentSuggestions);
    }


    public void refresh(String filter) {
        suggestions.clear();
        for (GroovyAbstractItem suggestion : currentSuggestions) {
            if (suggestion.getNameAndParams().toLowerCase().startsWith(filter.toLowerCase())) {
                suggestions.add(suggestion);
            }
        }
    }


    private Object getObjectFromScope(String currentLine, ScriptEngine engine) {
        Bindings engineScope = engine.getBindings(ScriptContext.ENGINE_SCOPE);
        Bindings globalScope = engine.getBindings(ScriptContext.GLOBAL_SCOPE);
        Object object = null;
        if (currentLine.length() == 0) {
            return null;
        }
        if (engineScope.containsKey(currentLine)) {
            object = engineScope.get(currentLine);
        } else if (globalScope.containsKey(currentLine)) {
            object = globalScope.get(currentLine);
        }
        return object;
    }


    private String[] getMethodsFromCurrentLine(String currentLine) {
        String currentInstruction = currentLine;
        if (!currentInstruction.contains(".")) {
            return new String[]{};
        }
        currentInstruction = currentInstruction.replaceAll("\\s", "");
        currentInstruction = currentInstruction.replaceAll("=", ";");
        currentInstruction = splitBraces(currentInstruction);
        String[] currentObjects = currentInstruction.split(";");
        return currentObjects[currentObjects.length - 1].split("\\.");
    }

    private String splitBraces(String currentInstruction) {
        StringBuilder result = new StringBuilder();
        for (int i = 1; i < currentInstruction.length(); i++) {
            if (currentInstruction.charAt(i - 1) == '(' && currentInstruction.charAt(i) != ')') {
                result.append(";");
            } else {
                result.append(currentInstruction.charAt(i - 1));
            }
        }
        return result.toString();
    }

    public void clear() {
        currentSuggestions.clear();
    }


}
