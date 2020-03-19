package de.prob2.ui.project.preferences;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import de.prob2.ui.json.JsonManager;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Preference {
	public static final Preference DEFAULT = new Preference("default", Collections.emptyMap());
	
	public static final JsonDeserializer<Preference> JSON_DESERIALIZER = Preference::new;

	private final StringProperty name;
	private Map<String, String> preferences;
	private final transient BooleanProperty changed = new SimpleBooleanProperty(false);

	public Preference(String name, Map<String, String> preferences) {
		this.name = new SimpleStringProperty(this, "name", name);
		this.preferences = preferences;
	}
	
	private Preference(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) {
		final JsonObject object = json.getAsJsonObject();
		this.name = JsonManager.checkDeserialize(context, object, "name", StringProperty.class);
		this.preferences = JsonManager.checkDeserialize(context, object, "preferences", new TypeToken<Map<String, String>>() {}.getType());
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
