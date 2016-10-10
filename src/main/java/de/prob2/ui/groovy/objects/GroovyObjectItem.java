package de.prob2.ui.groovy.objects;

import javafx.beans.property.SimpleStringProperty;

public class GroovyObjectItem {
	
	private final SimpleStringProperty name;
	private final Class<?> clazz;
	private final SimpleStringProperty clazzname;
	private final SimpleStringProperty value;
	private Object object;
	private GroovyClassStage classstage;
	
	public GroovyObjectItem(String name, Object object, GroovyClassStage classstage) {
		this.name = new SimpleStringProperty(name);
		this.object = object;
		this.clazz = object.getClass();
		this.clazzname = new SimpleStringProperty(clazz.getSimpleName());
		this.classstage = classstage;
		this.value = new SimpleStringProperty(object.toString());
		classstage.setClass(clazz);
	}
	
	public String getName() {
		return name.get();
	}
	
	public void setName(String name) {
		this.name.set(name);
	}
	
	public String getClazzname() {
		return clazzname.get();
	}
	
	public void setClass(String clazzname) {
		this.clazzname.set(clazzname);
	}
	
	public String getValue() {
		return value.get();
	}
	
	public void setValue(String value) {
		this.value.set(value);
	}
	
	public void show() {
		classstage.setTitle(clazz.getSimpleName());
		classstage.showMethodsAndFields(object);
		classstage.show();
		classstage.toFront();
	}
	
	public void close() {
		classstage.close();
	}

}
