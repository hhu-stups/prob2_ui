package de.prob2.ui.consoles.groovy.codecompletion;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;

import de.prob2.ui.codecompletion.GroovyCCItem;
import de.prob2.ui.consoles.groovy.GroovyMethodOption;
import de.prob2.ui.consoles.groovy.objects.GroovyAbstractItem;
import de.prob2.ui.consoles.groovy.objects.GroovyClassPropertyItem;
import de.prob2.ui.consoles.groovy.objects.GroovyObjectItem;

import org.codehaus.groovy.runtime.DefaultGroovyMethods;

import groovy.lang.MetaClass;
import groovy.lang.MetaMethod;
import groovy.lang.MetaProperty;

public class GroovyCodeCompletion {

	private static final Pattern DOT_WITH_WHITESPACES = Pattern.compile("\\s*\\.\\s*");
	private static final Map<String, String> GLOBAL_CLASSES = new HashMap<String, String>() {
		{
			this.put("BigDecimal", "java.math.BigDecimal");
			this.put("BigInteger", "java.math.BigInteger");
		}
	};
	private static final Set<String> GLOBAL_PACKAGES = new HashSet<>(Arrays.asList(
			"java.lang",
			"java.io",
			"java.net",
			"java.util",
			"groovy.lang",
			"groovy.util"
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

	public Collection<? extends GroovyCCItem> getSuggestions(String text) {
		String[] currentPrefixAndSuffix = extractPrefixAndSuffix(text);
		String namespace = currentPrefixAndSuffix[0];
		String member = currentPrefixAndSuffix[1];

		GroovyCodeCompletionHandler handler = new GroovyCodeCompletionHandler(namespace, member);
		handler.find();

		return handler.suggestions.stream()
				       .map(x -> new GroovyCCItem(member, x.getNameAndParams()))
				       .collect(Collectors.toList());
	}

	private class GroovyCodeCompletionHandler {

		final String namespace;
		final String member;
		final Set<GroovyAbstractItem> suggestions = new LinkedHashSet<>();

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
			Object value = resolveObject(namespace);
			if (value != null) {
				addAllMembers(value);
				return true;
			}

			Class<?> c = resolveClass(namespace);
			if (c != null) {
				addAllMembers(c);
				return true;
			}

			// TODO: multi-step resolution: java.lang.System.out

			return false;
		}

		private Object resolveObject(String namespace) {
			Object value = engine.get(namespace);
			if (value != null) {
				return value;
			}

			value = engine.getBindings(ScriptContext.GLOBAL_SCOPE).get(namespace);
			return value;
		}

		private Class<?> resolveClass(String namespace) {
			try {
				return Class.forName(namespace);
			} catch (ClassNotFoundException ignored) {
			}

			for (String p : GLOBAL_PACKAGES) {
				try {
					return Class.forName(p + "." + namespace);
				} catch (ClassNotFoundException ignored) {
				}
			}

			String className = GLOBAL_CLASSES.get(namespace);
			if (className != null) {
				try {
					return Class.forName(className);
				} catch (ClassNotFoundException ignored) {
				}
			}

			return null;
		}

		private void addAllMembers(Object o) {
			addAllMembers(DefaultGroovyMethods.getMetaClass(o), GroovyMethodOption.NONSTATIC);
		}

		private void addAllMembers(Class<?> c) {
			addAllMembers(DefaultGroovyMethods.getMetaClass(c), GroovyMethodOption.STATIC);
		}

		private void addAllMembers(MetaClass metaClass, GroovyMethodOption option) {
			for (Field f : metaClass.getTheClass().getFields()) {
				if (option.matches(f.getModifiers())) {
					addSuggestion(new GroovyClassPropertyItem(f));
				}
			}

			for (MetaProperty p : metaClass.getProperties()) {
				if (option.matches(p.getModifiers())) {
					addSuggestion(new GroovyClassPropertyItem(p));
				}
			}

			for (Method m : metaClass.getClass().getMethods()) {
				if (option.matches(m.getModifiers())) {
					addSuggestion(new GroovyClassPropertyItem(m));
				}
			}

			for (MetaMethod m : metaClass.getMetaMethods()) {
				if (option.matches(m.getModifiers())) {
					addSuggestion(new GroovyClassPropertyItem(m));
				}
			}
		}

		private void findGlobal() {
			for (Bindings b : new Bindings[] {
					engine.getBindings(ScriptContext.GLOBAL_SCOPE),
					engine.getBindings(ScriptContext.ENGINE_SCOPE)
			}) {
				b.forEach((k, v) -> {
					if (k != null && v != null && !k.isEmpty()) {
						addSuggestion(new GroovyObjectItem(k, v, null));
					}
				});
			}

			// TODO: suggest classes in GLOBAL_PACKAGES

			for (String name : GLOBAL_CLASSES.values()) {
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
