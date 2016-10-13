package de.prob2.ui.groovy.objects;

import javafx.beans.property.SimpleStringProperty;

public class GroovyAbstractItem {

	protected SimpleStringProperty name;
	
	public GroovyAbstractItem(String name) {
		this.name = new SimpleStringProperty(this, "name", name);
	}
	
	public String getName() {
		return name.get();
	}
	
	public void setName(String name) {
		this.name.set(name);
	}
	
}
