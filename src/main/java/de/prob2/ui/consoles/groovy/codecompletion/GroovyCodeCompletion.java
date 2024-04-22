package de.prob2.ui.consoles.groovy.codecompletion;

import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.script.ScriptEngine;

public final class GroovyCodeCompletion {

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

	private String[] extractPrefixAndSuffix(String text) {
		if (text.isEmpty()) {
			return new String[] { "", "" };
		}

		text = DOT_WITH_WHITESPACES.matcher(text).replaceAll(".");
		if (text.isEmpty()) {
			return new String[] { "", "" };
		}

		{
			int first = text.length();
			while (first > 0 && isDotOrGroovyIdentifierChar(text, first - 1)) {
				first--;
			}

			text = text.substring(first);
		}

		int lastDot = text.lastIndexOf('.');
		if (lastDot >= 0) {
			return new String[] { text.substring(0, lastDot), text.substring(lastDot + 1) };
		}

		return new String[] { "", text };
	}

	public List<? extends GroovyCCItem> getSuggestions(String text) {
		String[] currentPrefixAndSuffix = extractPrefixAndSuffix(text);
		String namespace = currentPrefixAndSuffix[0];
		String member = currentPrefixAndSuffix[1];

		GroovyCodeCompletionHandler handler = new GroovyCodeCompletionHandler(engine, namespace, member);
		handler.find();

		return handler.getSuggestions().stream()
			       .map(x -> new GroovyCCItem(member, x.getNameAndParams()))
			       .sorted(Comparator.comparing(Object::toString, String.CASE_INSENSITIVE_ORDER))
			       .collect(Collectors.toList());
	}
}
