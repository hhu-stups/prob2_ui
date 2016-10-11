package de.prob2.ui.groovy.objects;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import org.codehaus.groovy.reflection.CachedClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import groovy.lang.GroovyRuntimeException;
import groovy.lang.MetaMethod;
import groovy.lang.MetaProperty;
import groovy.lang.PropertyValue;
import javafx.beans.property.SimpleStringProperty;

public class GroovyClassPropertyItem {
	private static final Logger logger = LoggerFactory.getLogger(GroovyClassPropertyItem.class);
	
	private final SimpleStringProperty name;
	private final SimpleStringProperty params;
	private final SimpleStringProperty type;
	private final SimpleStringProperty origin;
	private final SimpleStringProperty modifier;
	private SimpleStringProperty declarer;
	private final SimpleStringProperty exception;
	private final SimpleStringProperty value;
	private final Class<?> returnTypeClass;
	private final boolean isMethod;

	public GroovyClassPropertyItem(Method m) {
		this.name = new SimpleStringProperty(this, "name", m.getName());
		final List<String> parameterNames = new ArrayList<>();
		for (Class<?> c : m.getParameterTypes()) {
			parameterNames.add(c.getSimpleName());
		}
		this.params = new SimpleStringProperty(this, "params", String.join(", ", parameterNames));
		this.type = new SimpleStringProperty(this, "type", m.getReturnType().getSimpleName());
		this.returnTypeClass = m.getReturnType();
		this.origin = new SimpleStringProperty(this, "origin", "JAVA");
		this.modifier = new SimpleStringProperty(this, "modifier", Modifier.toString(m.getModifiers()));
		this.declarer = new SimpleStringProperty(this, "declarer", m.getDeclaringClass().getSimpleName());
		final List<String> exceptionNames = new ArrayList<>();
		for (Class<?> c : m.getExceptionTypes()) {
			exceptionNames.add(c.getSimpleName());
		}
		this.exception = new SimpleStringProperty(this, "exception", String.join(", ", exceptionNames));
		this.value = new SimpleStringProperty(this, "value");
		this.isMethod = true;
	}

	public GroovyClassPropertyItem(Field f) {
		this.name = new SimpleStringProperty(this, "name", f.getName());
		this.params = new SimpleStringProperty(this, "params");
		this.type = new SimpleStringProperty(this, "type", f.getType().getSimpleName());
		this.returnTypeClass = f.getType();
		this.origin = new SimpleStringProperty(this, "origin", "JAVA");
		this.modifier = new SimpleStringProperty(this, "modifier", Modifier.toString(f.getModifiers()));
		this.declarer = new SimpleStringProperty(this, "declarer", f.getDeclaringClass().getSimpleName());
		this.exception = new SimpleStringProperty(this, "exception");
		this.value = new SimpleStringProperty(this, "value");
		try {
			this.value.set(f.get(null).toString());
		} catch (IllegalArgumentException | IllegalAccessException e) {
			logger.error("error creating property", e);
		}
		this.isMethod = false;
	}

	public GroovyClassPropertyItem(PropertyValue p) {
		this.name = new SimpleStringProperty(this, "name", p.getName());
		this.params = new SimpleStringProperty(this, "params");
		this.type = new SimpleStringProperty(this, "type", p.getType().getSimpleName());
		this.returnTypeClass = p.getType();
		this.origin = new SimpleStringProperty(this, "origin", "GROOVY");
		this.modifier = new SimpleStringProperty(this, "modifier", Modifier.toString(p.getType().getModifiers()));
		this.declarer = new SimpleStringProperty(this, "declarer", "n/a");
		if (p.getType().getDeclaringClass() != null) {
			this.declarer.set(p.getType().getDeclaringClass().getSimpleName());
		}
		this.exception = new SimpleStringProperty(this, "exception");
		this.value = new SimpleStringProperty(this, "value");
		try {
			if (p.getValue() != null) {
				this.value.set(p.getValue().toString());
			}
		} catch (GroovyRuntimeException | NoSuchElementException e) {
			logger.error("error creating property", e);
		}
		this.isMethod = false;
	}
	
	public GroovyClassPropertyItem(MetaProperty m) {
		this.name = new SimpleStringProperty(this, "name", m.getName());
		this.params = new SimpleStringProperty(this, "params");
		this.type = new SimpleStringProperty(this, "type", m.getType().getSimpleName());
		this.returnTypeClass = m.getType();
		this.origin = new SimpleStringProperty(this, "origin", "GROOVY");
		this.modifier = new SimpleStringProperty(this, "modifier", Modifier.toString(m.getType().getModifiers()));
		this.declarer = new SimpleStringProperty(this, "declarer", "n/a");
		if (m.getType().getDeclaringClass() != null) {
			this.declarer.set(m.getType().getDeclaringClass().getSimpleName());
		}
		this.exception = new SimpleStringProperty(this, "exception");
		this.value = new SimpleStringProperty(this, "value");
		this.isMethod = false;
	}

	public GroovyClassPropertyItem(MetaMethod m) {
		this.name = new SimpleStringProperty(this, "name", m.getName());
		final List<String> parameterNames = new ArrayList<>();
		for (CachedClass c : m.getParameterTypes()) {
			if(!c.isPrimitive()) {
				parameterNames.add(c.getTheClass().getSimpleName());
			} else {
				parameterNames.add(c.getName());
			}
		}
		this.params = new SimpleStringProperty(this, "params", String.join(", ", parameterNames));
		this.type = new SimpleStringProperty(this, "type", m.getReturnType().getSimpleName());
		this.returnTypeClass = m.getReturnType();
		this.origin = new SimpleStringProperty(this, "origin", "GROOVY");
		this.modifier = new SimpleStringProperty(this, "modifier", Modifier.toString(m.getModifiers()));
		this.declarer = new SimpleStringProperty(this, "declarer", m.getDeclaringClass().getTheClass().getSimpleName());
		this.exception = new SimpleStringProperty(this, "exception");
		this.value = new SimpleStringProperty(this, "value");
		this.isMethod = true;
	}

	public String getName() {
		return name.get();
	}

	public void setName(String name) {
		this.name.set(name);
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
	
	public String getNameAndParams() {
		String params = "";
		if(getParams() != null) {
			params = getParams();
		}
		if(!isMethod) {
			return getName();
		}
		return getName() + "(" + params + ")";
	}
	
	public String toString() {
		return getNameAndParams() +  " : " + getType() + " - " + getDeclarer();
	}
	
	public Class<? extends Object> getReturnTypeClass() {
		return returnTypeClass;
	}

}
