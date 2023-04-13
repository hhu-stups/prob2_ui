package de.prob2.ui.consoles.groovy.codecompletion;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;

import de.prob2.ui.codecompletion.CCItemTest;
import de.prob2.ui.consoles.groovy.objects.GroovyAbstractItem;
import de.prob2.ui.consoles.groovy.objects.GroovyObjectItem;

import groovy.lang.MetaClass;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.codehaus.groovy.runtime.DefaultGroovyMethodsSupport;
import org.codehaus.groovy.runtime.DefaultGroovyStaticMethods;

public class GroovyCodeCompletion {

	private static final Pattern DOT_WITH_WHITESPACES = Pattern.compile("\\s*\\.\\s*");
	// TODO: add more global classes
	private static final Set<String> GLOBAL_CLASSES = new HashSet<>(Arrays.asList(
			"java.math.BigDecimal",
			"java.math.BigInteger"
	));

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
			return new String[]{"", ""};
		}

		text = DOT_WITH_WHITESPACES.matcher(text).replaceAll(".");
		if (text.isEmpty()) {
			return new String[]{"", ""};
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
			return new String[]{text.substring(0, lastDot), text.substring(lastDot + 1)};
		}

		return new String[]{"", text};
	}

	public Collection<? extends CCItemTest> getSuggestions(String text) {
		String[] currentPrefixAndSuffix = extractPrefixAndSuffix(text);
		System.out.println("GroovyCodeCompletion.getSuggestions: prefix=" + Arrays.toString(currentPrefixAndSuffix));

		String namespace = currentPrefixAndSuffix[0];
		String member = currentPrefixAndSuffix[1];

		GroovyCodeCompletionHandler handler = new GroovyCodeCompletionHandler(namespace, member);
		handler.find();

		//GroovyCodeCompletionHandler completionHandler = new GroovyCodeCompletionHandler(suggestions);
		//completionHandler.handleObjectsFromEngineScopes(currentPrefix, engine);
		//completionHandler.handleStaticClasses(currentPrefix);
		//completionHandler.handleMethodsFromObjects(currentPrefix, engine);
		//completionHandler.refresh(currentPrefix);
		return handler.suggestions.stream()
				.map(x -> new CCItemTest(x.getNameAndParams()))
				.collect(Collectors.toList());
	}

	private class GroovyCodeCompletionHandler {

		final String namespace;
		final String member;
		final List<GroovyAbstractItem> suggestions = new ArrayList<>();

		GroovyCodeCompletionHandler(String namespace, String member) {
			this.namespace = namespace;
			this.member = member;
		}

		void find() {
			if (namespace.isEmpty()) {
				findGlobal();
			} else {
				findInNamespace(namespace);
			}
		}

		private boolean findInNamespace(String namespace) {
			System.out.println("find: namespace is " + namespace);
			Object value = engine.get(namespace);
			if (value == null) {
				value = engine.getBindings(ScriptContext.GLOBAL_SCOPE).get(namespace);
			}

			if (value != null) {
				System.out.println("object: " + value);
				addAllMembers(value);
				return true;
			}

			Class<?> c = null;
			try {
				c = Class.forName(namespace);
			} catch (ClassNotFoundException ignored) {
			}

			if (c != null) {
				System.out.println("class: " + c);
				return true;
			}

			return false;
		}

		private void addAllMembers(Object o) {
			MetaClass metaClass = DefaultGroovyMethods.getMetaClass(o);
			System.out.println("meta class of object: " + metaClass);
		}

		private void findGlobal() {
			System.out.println("find: namespace empty");
			for (Bindings b : new Bindings[]{
					engine.getBindings(ScriptContext.GLOBAL_SCOPE),
					engine.getBindings(ScriptContext.ENGINE_SCOPE)
			}) {
				b.forEach((k, v) -> {
					if (k != null && v != null && !k.isEmpty()) {
						addSuggestion(new GroovyObjectItem(k, v, null));
					}
				});
			}

			for (String name : GLOBAL_CLASSES) {
				Class<?> c;
				try {
					c = Class.forName(name);
				} catch (ClassNotFoundException ignored) {
					continue;
				}
				addSuggestion(new GroovyObjectItem(c.getSimpleName(), c, null));
			}
		}

		private void addSuggestion(GroovyAbstractItem item) {
			if (item.getNameAndParams().toLowerCase(Locale.ROOT).startsWith(member.toLowerCase(Locale.ROOT))) {
				suggestions.add(item);
			}
		}
	}
}
