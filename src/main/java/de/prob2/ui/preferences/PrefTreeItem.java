package de.prob2.ui.preferences;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public abstract class PrefTreeItem {
	private final StringProperty name;
	private final StringProperty changed;
	private final StringProperty value;
	private final ObjectProperty<ProBPreferenceType> valueType;
	private final StringProperty defaultValue;
	private final StringProperty description;
	
	protected PrefTreeItem(
		final String name,
		final String changed,
		final String value,
		final ProBPreferenceType valueType,
		final String defaultValue,
		final String description
	) {
		super();
		this.name = new SimpleStringProperty(this, "name", name);
		this.changed = new SimpleStringProperty(this, "changed", changed);
		this.value = new SimpleStringProperty(this, "value", value);
		this.valueType = new SimpleObjectProperty<>(this, "valueType", valueType);
		this.defaultValue = new SimpleStringProperty(this, "defaultValue", defaultValue);
		this.description = new SimpleStringProperty(this, "description", description);
	}
	
	public ReadOnlyStringProperty nameProperty() {
		return this.name;
	}
	
	public String getName() {
		return this.name.get();
	}
	
	public ReadOnlyStringProperty changedProperty() {
		return this.changed;
	}
	
	public String getChanged() {
		return this.changed.get();
	}
	
	public ReadOnlyStringProperty valueProperty() {
		return this.value;
	}
	
	public String getValue() {
		return this.value.get();
	}
	
	public ReadOnlyObjectProperty<ProBPreferenceType> valueTypeProperty() {
		return this.valueType;
	}
	
	public ProBPreferenceType getValueType() {
		return this.valueType.get();
	}

	public ReadOnlyStringProperty defaultValueProperty() {
		return this.defaultValue;
	}
	
	public String getDefaultValue() {
		return this.defaultValue.get();
	}

	public ReadOnlyStringProperty descriptionProperty() {
		return this.description;
	}
	
	public String getDescription() {
		return this.description.get();
	}
}
