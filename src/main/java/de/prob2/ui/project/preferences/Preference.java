package de.prob2.ui.project.preferences;

import java.util.Collections;
import java.util.Map;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Preference {
	public static final Preference DEFAULT = new Preference("default", Collections.emptyMap());

	private final StringProperty name;
	private Map<String, String> preferences;
	private transient BooleanProperty changed = new SimpleBooleanProperty(false);

	public Preference(String name, Map<String, String> preferences) {
		this.name = new SimpleStringProperty(this, "name", name);
		this.preferences = preferences;
	}
	
	public BooleanProperty changedProperty() {
		return changed;
	}
	
	public StringProperty nameProperty() {
		return this.name;
	}
	
	public String getName() {
		return this.nameProperty().get();
	}
	
	public void setName(String name) {
		this.nameProperty().set(name);
		this.changed.set(true);
	}
	
	public Map<String, String> getPreferences() {
		return preferences;
	}
	
	public void setPreferences(Map<String, String> preferences) {
		this.preferences = preferences;
		this.changed.set(true);
	}
	
	@Override
	public String toString() {
		return this.getName();
	}

	public void replaceMissingWithDefaults() {
		changed = new SimpleBooleanProperty(false);
	}
}
