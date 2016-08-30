package de.prob2.ui.groovy;

import javafx.beans.property.SimpleStringProperty;

public class GroovyObjectItem {
	
	private final SimpleStringProperty name;
	private final Class<? extends Object> clazz;
	private final SimpleStringProperty clazzname;
	private GroovyClassStage classstage;
	
	public GroovyObjectItem(String name, Class<? extends Object> clazz, GroovyClassStage classstage) {
		this.name = new SimpleStringProperty(name);
		this.clazz = clazz;
		this.clazzname = new SimpleStringProperty(clazz.getSimpleName());
		this.classstage = classstage;
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
	
	public void show() {
		classstage.setTitle(clazz.getSimpleName());
		classstage.showMethodsAndFields();
		classstage.show();
		classstage.toFront();
	}
	
	public void close() {
		classstage.close();
	}

}
