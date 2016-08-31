package de.prob2.ui.groovy;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.codehaus.groovy.reflection.CachedClass;

import groovy.lang.GroovyRuntimeException;
import groovy.lang.MetaMethod;
import groovy.lang.PropertyValue;
import javafx.beans.property.SimpleStringProperty;

public class GroovyClassPropertyItem {
	
	private final SimpleStringProperty name;
	private final SimpleStringProperty params;
	private final SimpleStringProperty type;
	private final SimpleStringProperty origin;
	private final SimpleStringProperty modifier;
	private SimpleStringProperty declarer;
	private final SimpleStringProperty exception;
	private SimpleStringProperty value;
	

	public GroovyClassPropertyItem(Method m) {
		this.name = new SimpleStringProperty(m.getName());
		String parameter ="";
		String exception = "";
		for(Class<? extends Object> c : m.getParameterTypes()) {
			parameter += c.getSimpleName() + ", ";
		}
		parameter = parameter.substring(0,Math.max(0,parameter.length()-2));
		this.params = new SimpleStringProperty(parameter);
		this.type = new SimpleStringProperty(m.getReturnType().getSimpleName());
		this.origin = new SimpleStringProperty("JAVA");
		this.modifier = new SimpleStringProperty(Modifier.toString(m.getModifiers()));
		this.declarer = new SimpleStringProperty(m.getDeclaringClass().getSimpleName());
		for(Class<? extends Object> c: m.getExceptionTypes()) {
			exception += c.getSimpleName() + ", ";
		}
		exception = exception.substring(0,Math.max(0,exception.length()-2));
		this.exception = new SimpleStringProperty(exception);
		this.value = new SimpleStringProperty();
	}
	
	public GroovyClassPropertyItem(Field f) {
		this.name = new SimpleStringProperty(f.getName());
		this.params = new SimpleStringProperty();
		this.type = new SimpleStringProperty(f.getType().getSimpleName());
		this.origin = new SimpleStringProperty("JAVA");
		this.modifier = new SimpleStringProperty(Modifier.toString(f.getModifiers()));
		this.declarer = new SimpleStringProperty(f.getDeclaringClass().getSimpleName());
		this.exception = new SimpleStringProperty();
		this.value = null;
		try {
			this.value = new SimpleStringProperty(f.get(null).toString());
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
	}
	
	public GroovyClassPropertyItem(PropertyValue p) {
		this.name = new SimpleStringProperty(p.getName());
		this.params = new SimpleStringProperty();
		this.type = new SimpleStringProperty(p.getType().getSimpleName());
		this.origin = new SimpleStringProperty("GROOVY");
		this.modifier = new SimpleStringProperty(Modifier.toString(p.getType().getModifiers()));
		this.declarer = new SimpleStringProperty("n/a");
		if(p.getType().getDeclaringClass() != null) {
			this.declarer = new SimpleStringProperty(p.getType().getDeclaringClass().getSimpleName());
		}
		this.exception = new SimpleStringProperty();
		this.value = new SimpleStringProperty();
		try {
			if(p.getValue() != null) {
				this.value = new SimpleStringProperty(p.getValue().toString());
			}
		} catch(GroovyRuntimeException e) {
			e.printStackTrace();
		}
	}
	
	public GroovyClassPropertyItem(MetaMethod m) {
		this.name = new SimpleStringProperty(m.getName());
		String parameter ="";
		for(CachedClass c : m.getParameterTypes()) {
			parameter += c.getName()+ ", ";
		}
		parameter = parameter.substring(0,Math.max(0,parameter.length()-2));
		this.params = new SimpleStringProperty(parameter);
		this.type = new SimpleStringProperty(m.getReturnType().getSimpleName());
		this.origin = new SimpleStringProperty("GROOVY");
		this.modifier = new SimpleStringProperty(Modifier.toString(m.getModifiers()));
		this.declarer = new SimpleStringProperty(m.getDeclaringClass().getName());
		this.exception = new SimpleStringProperty();
		this.value = new SimpleStringProperty();
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
	
}
