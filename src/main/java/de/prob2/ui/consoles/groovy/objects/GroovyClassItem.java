package de.prob2.ui.consoles.groovy.objects;

import javafx.beans.property.SimpleStringProperty;

public final class GroovyClassItem {
	private final SimpleStringProperty attribute;
	private final SimpleStringProperty value;

	public GroovyClassItem(String attribute, String value) {
		this.attribute = new SimpleStringProperty(this, "attribute", attribute);
		this.value = new SimpleStringProperty(this, "value", value);
	}

	public String getAttribute() {
		return attribute.get();
	}

	public void setAttribute(String attribute) {
		this.attribute.set(attribute);
	}

	public String getValue() {
		return this.value.get();
	}

	public void setValue(String value) {
		this.value.set(value);
	}

}
