package de.prob2.ui.consoles.groovy.codecompletion;

import java.util.Objects;

import de.prob2.ui.consoles.groovy.GroovyMethodOption;

import org.codehaus.groovy.runtime.DefaultGroovyMethods;

import groovy.lang.MetaClass;

public final class TypedValue implements Resolved {

	private final Class<?> type;
	private final Object value;

	public TypedValue(Object value) {
		Objects.requireNonNull(value);
		this.type = value.getClass();
		this.value = value;
	}

	public TypedValue(Class<?> type) {
		this.type = Objects.requireNonNull(type);
		this.value = null;
	}

	public TypedValue(Class<?> type, Object value) {
		if (value != null && !type.isAssignableFrom(value.getClass())) {
			throw new IllegalArgumentException();
		}

		this.type = type;
		this.value = value;
	}

	public Class<?> getType() {
		return type;
	}

	public Object getValue() {
		return value;
	}

	@Override
	public MetaClass getMetaClass() {
		if (value != null) {
			return DefaultGroovyMethods.getMetaClass(value);
		} else {
			return DefaultGroovyMethods.getMetaClass(type);
		}
	}

	@Override
	public Resolved resolve(String name) {
		return ResolverUtils.getMember(value, getMetaClass(), GroovyMethodOption.NONSTATIC, name);
	}
}
