package de.prob2.ui.states;

import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class StateTreeItem<T> {
	private final StringProperty name;
	private final StringProperty value;
	private final StringProperty previousValue;
	private final T contents;
	
	protected StateTreeItem(final String name, final String value, final String previousValue, final T contents) {
		super();
		this.name = new SimpleStringProperty(this, "name", name);
		this.value = new SimpleStringProperty(this, "value", value);
		this.previousValue = new SimpleStringProperty(this, "previousValue", previousValue);
		this.contents = contents;
	}
	
	public ReadOnlyStringProperty nameProperty() {
		return this.name;
	}
	
	public String getName() {
		return this.name.get();
	}
	
	public ReadOnlyStringProperty valueProperty() {
		return this.value;
	}
	
	public String getValue() {
		return this.value.get();
	}
	
	public ReadOnlyStringProperty previousValueProperty() {
		return this.previousValue;
	}
	
	public String getPreviousValue() {
		return this.previousValue.get();
	}
	
	public T getContents() {
		return this.contents;
	}
}
