package de.prob2.ui.consoles.groovy.codecompletion;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;

import de.prob2.ui.consoles.groovy.objects.GroovyAbstractItem;
import de.prob2.ui.consoles.groovy.objects.GroovyObjectItem;

final class GroovyCodeCompletionHandler {

	private static final Map<String, String> GLOBAL_CLASSES = Map.of(
			"BigDecimal", "java.math.BigDecimal",
			"BigInteger", "java.math.BigInteger"
	);
	private static final Set<String> GLOBAL_PACKAGES = Set.of(
			"java.lang",
			"java.io",
			"java.net",
			"java.util",
			"groovy.lang",
			"groovy.util"
	);

	private final ScriptEngine engine;
	private final String namespace;
	private final String member;
	private final Set<GroovyAbstractItem> suggestions = new LinkedHashSet<>();

	GroovyCodeCompletionHandler(ScriptEngine engine, String namespace, String member) {
		this.engine = engine;
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

	private TypedValue resolveGlobalObject(String name) {
		for (Bindings b : new Bindings[] {
				engine.getBindings(ScriptContext.ENGINE_SCOPE),
				engine.getBindings(ScriptContext.GLOBAL_SCOPE)
		}) {
			Object value = b.get(name);
			if (value != null) {
				return new TypedValue(value);
			} else if (b.containsKey(name)) {
				return new TypedValue(Object.class);
			}
		}

		return null;
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

	private void findInNamespace(String namespace) {
		Resolved resolved = resolve(namespace);
		if (resolved != null) {
			addSuggestions(ResolverUtils.getAllMembers(resolved));
		}
	}

	private Resolved resolve(String name) {
		{
			TypedValue value = resolveGlobalObject(name);
			if (value != null) {
				return value;
			}

			Class<?> c = resolveClass(name);
			if (c != null) {
				return new Clazz(c);
			}
		}

		int lastDot = name.lastIndexOf('.');
		if (lastDot < 0) {
			return null;
		}

		String prefix = name.substring(0, lastDot);
		String suffix = name.substring(lastDot + 1);

		Resolved prefixResolved = resolve(prefix);
		if (prefixResolved != null) {
			return prefixResolved.resolve(suffix);
		}

		return null;
	}

	private void addSuggestion(GroovyAbstractItem item) {
		if (item.getNameAndParams().toLowerCase(Locale.ROOT).startsWith(member.toLowerCase(Locale.ROOT))) {
			suggestions.add(item);
		}
	}

	private void addSuggestions(Iterable<? extends GroovyAbstractItem> items) {
		for (GroovyAbstractItem item : items) {
			addSuggestion(item);
		}
	}

	public Set<GroovyAbstractItem> getSuggestions() {
		return suggestions;
	}
}
