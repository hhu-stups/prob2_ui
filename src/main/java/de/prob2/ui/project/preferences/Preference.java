package de.prob2.ui.project.preferences;

import java.util.Map;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public class Preference {

	private String name;
	private Map<String, String> preferences;
	private transient BooleanProperty changed = new SimpleBooleanProperty(false);

	public Preference(String name, Map<String, String> preferences) {
		this.name = name;
		this.preferences = preferences;
	}
	
	public BooleanProperty changedProperty() {
		return changed;
	}
	
	public String getName() {
		return name;
	}
	
	public Map<String, String> getPreferences() {
		return preferences;
	}
	
	public void setName(String name) {
		this.name = name;
		this.changed.set(true);
	}
	
	public void setPreferences(Map<String, String> preferences) {
		this.preferences = preferences;
		this.changed.set(true);
	}
	
	@Override
	public String toString() {
		return this.name;
	}

	public void replaceMissingWithDefaults() {
		changed = new SimpleBooleanProperty(false);
	}
}
