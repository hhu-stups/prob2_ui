package de.prob2.ui.groovy;

import javafx.beans.property.SimpleStringProperty;

public class GroovyObjectItem {
	
	private final SimpleStringProperty name;
	
	public GroovyObjectItem(String name) {
		this.name = new SimpleStringProperty(name);
	}
	
	public String getName() {
		return name.get();
	}
	
	public void setName(String name) {
		this.name.set(name);
	}

}
