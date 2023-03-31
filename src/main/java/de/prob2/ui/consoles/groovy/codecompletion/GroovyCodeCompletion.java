package de.prob2.ui.consoles.groovy.codecompletion;

import java.util.Collection;
import java.util.Collections;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.script.ScriptEngine;

import de.prob2.ui.codecompletion.CCItemTest;
import de.prob2.ui.consoles.groovy.objects.GroovyAbstractItem;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class GroovyCodeCompletion {

	private static final Pattern DOT_WITH_WHITESPACES = Pattern.compile("\\s*\\.\\s*");

	private final ScriptEngine engine;

	public GroovyCodeCompletion(ScriptEngine engine) {
		this.engine = engine;
	}

	private static boolean isGroovyIdentifierStart(char c) {
		return Character.isJavaIdentifierStart(c) && !Character.isIdentifierIgnorable(c);
	}

	private static boolean isGroovyIdentifierPart(char c) {
		return Character.isJavaIdentifierPart(c) && !Character.isIdentifierIgnorable(c);
	}

	private static boolean isDotOrGroovyIdentifierChar(String text, int index) {
		char c = text.charAt(index);
		if (c == '.') {
			return true;
		}

		if (index == 0) {
			return isGroovyIdentifierStart(c);
		} else {
			char p = text.charAt(index - 1);
			if (isGroovyIdentifierStart(p) || isGroovyIdentifierPart(p)) {
				return isGroovyIdentifierPart(c);
			} else {
				return isGroovyIdentifierStart(c);
			}
		}
	}

	private String extractPrefix(String text) {
		if (text.isEmpty()) {
			return "";
		}

		text = DOT_WITH_WHITESPACES.matcher(text).replaceAll(".");
		if (text.isEmpty()) {
			return "";
		}

		{
			int first = text.length();
			while (first > 0 && isDotOrGroovyIdentifierChar(text, first - 1)) {
				first--;
			}

			text = text.substring(first);
		}

		return text;
	}

	public Collection<? extends CCItemTest> getSuggestions(String text) {
		String currentPrefix = extractPrefix(text);
		System.out.println("GroovyCodeCompletion.getSuggestions: prefix=" + currentPrefix);
		if (currentPrefix.startsWith(".")) {
			// if the current expression starts with a dot it is probably malformed, return nothing
			return Collections.emptyList();
		}

		ObservableList<GroovyAbstractItem> suggestions = FXCollections.observableArrayList();
		GroovyCodeCompletionHandler completionHandler = new GroovyCodeCompletionHandler(suggestions);
		completionHandler.handleObjectsFromEngineScopes(currentPrefix, engine);
		completionHandler.handleStaticClasses(currentPrefix);
		completionHandler.handleMethodsFromObjects(currentPrefix, engine);
		completionHandler.refresh(currentPrefix);
		return suggestions.stream()
				       .map(x -> new CCItemTest(x.getNameAndParams()))
				       .collect(Collectors.toList());
	}
}
