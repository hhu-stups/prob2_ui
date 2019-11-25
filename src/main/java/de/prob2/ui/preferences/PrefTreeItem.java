package de.prob2.ui.preferences;

import com.google.common.base.MoreObjects;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

abstract class PrefTreeItem {
	public static class Category extends PrefTreeItem {
		Category(String name) {
			super(name, "", "", null, "", "");
		}
	}
	
	public static class Preference extends PrefTreeItem {
		Preference(
			final String name,
			final String value,
			final ProBPreferenceType valueType,
			final String defaultValue,
			final String description
		) {
			super(name, value.equals(defaultValue) ? "" : "*", value, valueType, defaultValue, description);
		}
	}
	
	private final StringProperty name;
	private final StringProperty changed;
	private final StringProperty value;
	private final ObjectProperty<ProBPreferenceType> valueType;
	private final StringProperty defaultValue;
	private final StringProperty description;
	
	PrefTreeItem(
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
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
			.add("name", this.getName())
			.add("changed", this.getChanged())
			.add("value", this.getValue())
			.add("valueType", this.getValueType())
			.add("defaultValue", this.getDefaultValue())
			.add("description", this.getDescription())
			.toString();
	}
}
