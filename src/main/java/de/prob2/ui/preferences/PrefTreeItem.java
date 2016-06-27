package de.prob2.ui.preferences;

import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public abstract class PrefTreeItem {
	protected StringProperty name;
	public ReadOnlyStringProperty nameProperty() {
		return this.name;
	}
	public String getName() {
		return this.name.get();
	}

	protected StringProperty value;
	public ReadOnlyStringProperty valueProperty() {
		return this.value;
	}
	public String getValue() {
		return this.value.get();
	}

	protected StringProperty defaultValue;
	public ReadOnlyStringProperty defaultValueProperty() {
		return this.defaultValue;
	}
	public String getDefaultValue() {
		return this.defaultValue.get();
	}

	protected StringProperty description;
	public ReadOnlyStringProperty descriptionProperty() {
		return this.description;
	}
	public String getDescription() {
		return this.description.get();
	}

	public PrefTreeItem(String name, String value, String defaultValue, String description) {
		super();
		this.name = new SimpleStringProperty(name);
		this.value = new SimpleStringProperty(value);
		this.defaultValue = new SimpleStringProperty(defaultValue);
		this.description = new SimpleStringProperty(description);
	}

	public void updateValue(final Preferences prefs) {}
}
