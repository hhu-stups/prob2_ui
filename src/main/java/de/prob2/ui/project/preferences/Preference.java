package de.prob2.ui.project.preferences;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public final class Preference {
	public static final Preference DEFAULT = new Preference("default", Collections.emptyMap());
	
	private final StringProperty name;
	private Map<String, String> preferences;
	@JsonIgnore
	private final BooleanProperty changed = new SimpleBooleanProperty(false);

	@JsonCreator
	public Preference(
		@JsonProperty("name") final String name,
		@JsonProperty("preferences") final Map<String, String> preferences
	) {
		this.name = new SimpleStringProperty(this, "name", Objects.requireNonNull(name, "name"));
		this.preferences = Objects.requireNonNull(preferences, "preferences");
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
}
