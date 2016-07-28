package de.prob2.ui.preferences;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public abstract class PrefTreeItem {
	protected final StringProperty name;
	public ReadOnlyStringProperty nameProperty() {
		return this.name;
	}
	public String getName() {
		return this.name.get();
	}
	
	protected final StringProperty changed;
	public ReadOnlyStringProperty changedProperty() {
		return this.changed;
	}
	public String getChanged() {
		return this.changed.get();
	}
	
	protected final StringProperty value;
	public ReadOnlyStringProperty valueProperty() {
		return this.value;
	}
	public String getValue() {
		return this.value.get();
	}
	
	protected final ObjectProperty<Class<?>> valueType;
	public ReadOnlyObjectProperty<Class<?>> valueTypeProperty() {
		return this.valueType;
	}
	public Class<?> getValueType() {
		return this.valueType.get();
	}

	protected final StringProperty defaultValue;
	public ReadOnlyStringProperty defaultValueProperty() {
		return this.defaultValue;
	}
	public String getDefaultValue() {
		return this.defaultValue.get();
	}

	protected final StringProperty description;
	public ReadOnlyStringProperty descriptionProperty() {
		return this.description;
	}
	public String getDescription() {
		return this.description.get();
	}

	public PrefTreeItem(
		final String name,
		final String changed,
		final String value,
		final Class<?> valueType,
		final String defaultValue,
		final String description
	) {
		super();
		this.name = new SimpleStringProperty(name);
		this.changed = new SimpleStringProperty(changed);
		this.value = new SimpleStringProperty(value);
		this.valueType = new SimpleObjectProperty<>(valueType);
		this.defaultValue = new SimpleStringProperty(defaultValue);
		this.description = new SimpleStringProperty(description);
	}

	public void updateValue(final Preferences prefs) {}
}
