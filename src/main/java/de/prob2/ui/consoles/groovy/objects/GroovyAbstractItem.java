package de.prob2.ui.consoles.groovy.objects;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class GroovyAbstractItem {
	protected final StringProperty name;

	public GroovyAbstractItem(String name) {
		this.name = new SimpleStringProperty(this, "name", name);
	}

	public String getName() {
		return name.get();
	}

	public void setName(String name) {
		this.name.set(name);
	}

	public String getNameAndParams() {
		return getName();
	}

	@Override
	public String toString() {
		return getName();
	}

}
