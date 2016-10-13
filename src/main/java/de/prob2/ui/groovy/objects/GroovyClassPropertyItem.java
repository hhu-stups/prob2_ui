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

public class GroovyClassPropertyItem extends GroovyAbstractItem {
	private static final Logger logger = LoggerFactory.getLogger(GroovyClassPropertyItem.class);
	
	private final SimpleStringProperty params;
	private final SimpleStringProperty type;
	private final SimpleStringProperty origin;
	private final SimpleStringProperty modifier;
	private SimpleStringProperty declarer;
	private final SimpleStringProperty exception;
	private final SimpleStringProperty value;
	private final Class<?> returnTypeClass;
	private final boolean isMethod;
	
	private static final String PARAMS = "params";
	private static final String TYPE = "type";
	private static final String ORIGIN = "orgin";
	private static final String MODIFIER = "modifier";
	private static final String DECLARER = "declarer";
	private static final String EXCEPTION = "exception";
	private static final String VALUE = "value";

	public GroovyClassPropertyItem(Method m) {
		super(m.getName());
		final List<String> parameterNames = new ArrayList<>();
		for (Class<?> c : m.getParameterTypes()) {
			parameterNames.add(c.getSimpleName());
		}
		this.params = new SimpleStringProperty(this, PARAMS, String.join(", ", parameterNames));
		this.type = new SimpleStringProperty(this, TYPE, m.getReturnType().getSimpleName());
		this.returnTypeClass = m.getReturnType();
		this.origin = new SimpleStringProperty(this, ORIGIN, "JAVA");
		this.modifier = new SimpleStringProperty(this, MODIFIER, Modifier.toString(m.getModifiers()));
		this.declarer = new SimpleStringProperty(this, DECLARER, m.getDeclaringClass().getSimpleName());
		final List<String> exceptionNames = new ArrayList<>();
		for (Class<?> c : m.getExceptionTypes()) {
			exceptionNames.add(c.getSimpleName());
		}
		this.exception = new SimpleStringProperty(this, EXCEPTION, String.join(", ", exceptionNames));
		this.value = new SimpleStringProperty(this, VALUE);
		this.isMethod = true;
	}

	public GroovyClassPropertyItem(Field f) {
		super(f.getName());
		this.params = new SimpleStringProperty(this, PARAMS);
		this.type = new SimpleStringProperty(this, TYPE, f.getType().getSimpleName());
		this.returnTypeClass = f.getType();
		this.origin = new SimpleStringProperty(this, ORIGIN, "JAVA");
		this.modifier = new SimpleStringProperty(this, MODIFIER, Modifier.toString(f.getModifiers()));
		this.declarer = new SimpleStringProperty(this, DECLARER, f.getDeclaringClass().getSimpleName());
		this.exception = new SimpleStringProperty(this, EXCEPTION);
		this.value = new SimpleStringProperty(this, VALUE);
		try {
			this.value.set(f.get(null).toString());
		} catch (IllegalArgumentException | IllegalAccessException e) {
			logger.error("error creating property", e);
		}
		this.isMethod = false;
	}

	public GroovyClassPropertyItem(PropertyValue p) {
		super(p.getName());
		this.params = new SimpleStringProperty(this, PARAMS);
		this.type = new SimpleStringProperty(this, TYPE, p.getType().getSimpleName());
		this.returnTypeClass = p.getType();
		this.origin = new SimpleStringProperty(this, ORIGIN, "GROOVY");
		this.modifier = new SimpleStringProperty(this, MODIFIER, Modifier.toString(p.getType().getModifiers()));
		this.declarer = new SimpleStringProperty(this, DECLARER, "n/a");
		if (p.getType().getDeclaringClass() != null) {
			this.declarer.set(p.getType().getDeclaringClass().getSimpleName());
		}
		this.exception = new SimpleStringProperty(this, EXCEPTION);
		this.value = new SimpleStringProperty(this, VALUE);
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
		super(m.getName());
		this.params = new SimpleStringProperty(this, PARAMS);
		this.type = new SimpleStringProperty(this, TYPE, m.getType().getSimpleName());
		this.returnTypeClass = m.getType();
		this.origin = new SimpleStringProperty(this, ORIGIN, "GROOVY");
		this.modifier = new SimpleStringProperty(this, MODIFIER, Modifier.toString(m.getType().getModifiers()));
		this.declarer = new SimpleStringProperty(this, DECLARER, "n/a");
		if (m.getType().getDeclaringClass() != null) {
			this.declarer.set(m.getType().getDeclaringClass().getSimpleName());
		}
		this.exception = new SimpleStringProperty(this, EXCEPTION);
		this.value = new SimpleStringProperty(this, VALUE);
		this.isMethod = false;
	}

	public GroovyClassPropertyItem(MetaMethod m) {
		super(m.getName());
		final List<String> parameterNames = new ArrayList<>();
		for (CachedClass c : m.getParameterTypes()) {
			if(!c.isPrimitive()) {
				parameterNames.add(c.getTheClass().getSimpleName());
			} else {
				parameterNames.add(c.getName());
			}
		}
		this.params = new SimpleStringProperty(this, PARAMS, String.join(", ", parameterNames));
		this.type = new SimpleStringProperty(this, TYPE, m.getReturnType().getSimpleName());
		this.returnTypeClass = m.getReturnType();
		this.origin = new SimpleStringProperty(this, ORIGIN, "GROOVY");
		this.modifier = new SimpleStringProperty(this, MODIFIER, Modifier.toString(m.getModifiers()));
		this.declarer = new SimpleStringProperty(this, DECLARER, m.getDeclaringClass().getTheClass().getSimpleName());
		this.exception = new SimpleStringProperty(this, EXCEPTION);
		this.value = new SimpleStringProperty(this, VALUE);
		this.isMethod = true;
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
		String parameters = "";
		if(getParams() != null) {
			parameters = getParams();
		}
		if(!isMethod) {
			return getName();
		}
		return getName() + "(" + parameters + ")";
	}
	
	@Override
	public String toString() {
		return getNameAndParams() +  " : " + getType() + " - " + getDeclarer();
	}
	
	public Class<? extends Object> getReturnTypeClass() {
		return returnTypeClass;
	}

}
