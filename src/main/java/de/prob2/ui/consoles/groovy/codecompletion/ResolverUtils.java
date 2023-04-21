package de.prob2.ui.consoles.groovy.codecompletion;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import de.prob2.ui.consoles.groovy.GroovyMethodOption;
import de.prob2.ui.consoles.groovy.objects.GroovyAbstractItem;
import de.prob2.ui.consoles.groovy.objects.GroovyClassPropertyItem;

import org.codehaus.groovy.runtime.DefaultGroovyMethods;

import groovy.lang.MetaClass;
import groovy.lang.MetaMethod;
import groovy.lang.MetaProperty;

final class ResolverUtils {

	private ResolverUtils() {
	}

	static TypedValue getMember(Object o, MetaClass metaClass, GroovyMethodOption option, String name) {
		try {
			Field field = metaClass.getTheClass().getField(name);
			if (option.matches(field.getModifiers())) {
				Object value;
				try {
					value = field.get(o);
				} catch (Exception ignored) {
					value = null;
				}
				return new TypedValue(field.getType(), value);
			}
		} catch (NoSuchFieldException ignored) {
		}

		MetaProperty property = metaClass.getMetaProperty(name);
		if (property != null && option.matches(property.getModifiers())) {
			Object value;
			try {
				value = property.getProperty(o);
			} catch (Exception ignored) {
				value = null;
			}
			return new TypedValue(property.getType(), value);
		}

		return null;
	}

	static List<GroovyAbstractItem> getAllMembers(Resolved r) {
		if (r instanceof Clazz) {
			return getAllMembers((Clazz) r);
		} else if (r instanceof TypedValue) {
			return getAllMembers((TypedValue) r);
		} else {
			throw new IllegalArgumentException();
		}
	}

	static List<GroovyAbstractItem> getAllMembers(Clazz c) {
		return getAllMembers(c.getClazz());
	}

	static List<GroovyAbstractItem> getAllMembers(TypedValue tv) {
		if (tv.getValue() != null) {
			return getAllMembers(tv.getValue());
		} else {
			return getAllMembers(DefaultGroovyMethods.getMetaClass(tv.getType()), GroovyMethodOption.NONSTATIC);
		}
	}

	static List<GroovyAbstractItem> getAllMembers(Object o) {
		return getAllMembers(DefaultGroovyMethods.getMetaClass(o), GroovyMethodOption.NONSTATIC);
	}

	static List<GroovyAbstractItem> getAllMembers(Class<?> c) {
		return getAllMembers(DefaultGroovyMethods.getMetaClass(c), GroovyMethodOption.STATIC);
	}

	static List<GroovyAbstractItem> getAllMembers(MetaClass metaClass, GroovyMethodOption option) {
		List<GroovyAbstractItem> candidates = new ArrayList<>();

		for (Field f : metaClass.getTheClass().getFields()) {
			if (option.matches(f.getModifiers())) {
				candidates.add(new GroovyClassPropertyItem(f));
			}
		}

		for (MetaProperty p : metaClass.getProperties()) {
			if (option.matches(p.getModifiers())) {
				candidates.add(new GroovyClassPropertyItem(p));
			}
		}

		for (Method m : metaClass.getClass().getMethods()) {
			if (option.matches(m.getModifiers())) {
				candidates.add(new GroovyClassPropertyItem(m));
			}
		}

		for (MetaMethod m : metaClass.getMetaMethods()) {
			if (option.matches(m.getModifiers())) {
				candidates.add(new GroovyClassPropertyItem(m));
			}
		}

		return candidates;
	}
}
