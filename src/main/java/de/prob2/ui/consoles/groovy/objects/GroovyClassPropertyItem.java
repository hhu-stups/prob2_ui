package de.prob2.ui.consoles.groovy.objects;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.codehaus.groovy.reflection.CachedClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import groovy.lang.GroovyRuntimeException;
import groovy.lang.MetaMethod;
import groovy.lang.MetaProperty;
import groovy.lang.PropertyValue;

public final class GroovyClassPropertyItem extends GroovyAbstractItem {
	private static final Logger LOGGER = LoggerFactory.getLogger(GroovyClassPropertyItem.class);

	private final StringProperty params;
	private final StringProperty type;
	private final StringProperty origin;
	private final StringProperty modifier;
	private final StringProperty declarer;
	private final StringProperty exception;
	private final StringProperty value;
	private final Class<?> returnTypeClass;
	private final boolean isMethod;

	private GroovyClassPropertyItem(String name, Class<?> returnTypeClass, boolean isMethod) {
		super(name);

		params = new SimpleStringProperty(this, "params");
		type = new SimpleStringProperty(this, "type");
		origin = new SimpleStringProperty(this, "origin");
		modifier = new SimpleStringProperty(this, "modifier");
		declarer = new SimpleStringProperty(this, "declarer");
		exception = new SimpleStringProperty(this, "exception");
		value = new SimpleStringProperty(this, "value");
		this.returnTypeClass = returnTypeClass;
		this.isMethod = isMethod;
	}

	public GroovyClassPropertyItem(Method m) {
		this(m.getName(), m.getReturnType(), true);

		final List<String> parameterNames = new ArrayList<>();
		for (Class<?> c : m.getParameterTypes()) {
			parameterNames.add(c.getSimpleName());
		}
		this.params.set(String.join(", ", parameterNames));
		this.type.set(m.getReturnType().getSimpleName());
		this.origin.set("JAVA");
		this.modifier.set(Modifier.toString(m.getModifiers()));
		this.declarer.set(m.getDeclaringClass().getSimpleName());
		final List<String> exceptionNames = new ArrayList<>();
		for (Class<?> c : m.getExceptionTypes()) {
			exceptionNames.add(c.getSimpleName());
		}
		this.exception.set(String.join(", ", exceptionNames));
	}

	public GroovyClassPropertyItem(Field f) {
		this(f.getName(), f.getType(), false);

		this.type.set(f.getType().getSimpleName());
		this.origin.set("JAVA");
		this.modifier.set(Modifier.toString(f.getModifiers()));
		this.declarer.set(f.getDeclaringClass().getSimpleName());
		try {
			this.value.set(f.get(null).toString());
		} catch (IllegalArgumentException | IllegalAccessException e) {
			LOGGER.error("error creating property", e);
		}
	}

	public GroovyClassPropertyItem(PropertyValue p) {
		this(p.getName(), p.getType(), false);

		this.type.set(p.getType().getSimpleName());
		this.origin.set("GROOVY");
		this.modifier.set(Modifier.toString(p.getType().getModifiers()));
		this.declarer.set("n/a");
		try {
			if (p.getValue() != null) {
				this.value.set(p.getValue().toString());
			}
		} catch (GroovyRuntimeException | NoSuchElementException e) {
			LOGGER.error("error creating property", e);
		}
	}

	public GroovyClassPropertyItem(MetaProperty m) {
		this(m.getName(), m.getType(), false);

		this.type.set(m.getType().getSimpleName());
		this.origin.set("GROOVY");
		this.modifier.set(Modifier.toString(m.getType().getModifiers()));
		this.declarer.set("n/a");
	}

	public GroovyClassPropertyItem(MetaMethod m) {
		this(m.getName(), m.getReturnType(), true);

		final List<String> parameterNames = new ArrayList<>();
		for (CachedClass c : m.getParameterTypes()) {
			if (c.isPrimitive()) {
				parameterNames.add(c.getName());
			} else {
				parameterNames.add(c.getTheClass().getSimpleName());
			}
		}
		this.params.set(String.join(", ", parameterNames));
		this.type.set(m.getReturnType().getSimpleName());
		this.origin.set("GROOVY");
		this.modifier.set(Modifier.toString(m.getModifiers()));
		this.declarer.set(m.getDeclaringClass().getTheClass().getSimpleName());
	}

	public String getParams() {
		return params.get();
	}

	public void setParams(String params) {
		this.params.set(params);
	}

	public String getType() {
		return type.get();
	}

	public void setType(String type) {
		this.type.set(type);
	}

	public String getOrigin() {
		return origin.get();
	}

	public void setOrigin(String origin) {
		this.type.set(origin);
	}

	public String getModifier() {
		return modifier.get();
	}

	public void setModifier(String modifier) {
		this.modifier.set(modifier);
	}

	public String getDeclarer() {
		return declarer.get();
	}

	public void setDeclarer(String declarer) {
		this.declarer.set(declarer);
	}

	public String getException() {
		return exception.get();
	}

	public void setException(String exception) {
		this.exception.set(exception);
	}

	public String getValue() {
		return value.get();
	}

	public void setValue(String value) {
		this.value.set(value);
	}

	@Override
	public String getNameAndParams() {
		final String parameters = isMethod ? "(" + getParams() + ")" : "";
		return getName() + parameters;
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, isMethod, params, type, declarer);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof GroovyClassPropertyItem other)) {
			return false;
		}
		return other.name.get().equals(name.get()) &&
				       other.isMethod == this.isMethod &&
				       other.params.get().equals(params.get()) &&
				       other.type.get().equals(this.type.get()) && other.declarer.get().equals(this.declarer.get());
	}

	@Override
	public String toString() {
		return getNameAndParams() + " : " + getType() + " - " + getDeclarer();
	}

	public Class<?> getReturnTypeClass() {
		return returnTypeClass;
	}

}
