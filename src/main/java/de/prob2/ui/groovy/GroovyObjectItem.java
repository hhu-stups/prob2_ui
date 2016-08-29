package de.prob2.ui.groovy;

import javafx.beans.property.SimpleStringProperty;

public class GroovyObjectItem {
	
	private final SimpleStringProperty name;
	private final SimpleStringProperty clazz;
	
	public GroovyObjectItem(String name, String clazz) {
		this.name = new SimpleStringProperty(name);
		this.clazz = new SimpleStringProperty(clazz);
	}
	
	public String getName() {
		return name.get();
	}
	
	public void setName(String name) {
		this.name.set(name);
	}
	
	public String getClazz() {
		return clazz.get();
	}
	
	public void setClass(String clazz) {
		this.clazz.set(clazz);
	}

}
