package de.prob2.ui.project.preferences;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

public final class Preference {
	public static final Preference DEFAULT = new Preference("default", Collections.emptyMap());
	
	private final String name;
	private final Map<String, String> preferences;

	@JsonCreator
	public Preference(
		@JsonProperty("name") final String name,
		@JsonProperty("preferences") final Map<String, String> preferences
	) {
		this.name = Objects.requireNonNull(name, "name");
		this.preferences = Objects.requireNonNull(preferences, "preferences");
	}
	
	public String getName() {
		return this.name;
	}
	
	public Map<String, String> getPreferences() {
		return Collections.unmodifiableMap(this.preferences);
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
			.add("name", this.getName())
			.add("preferences", this.getPreferences())
			.toString();
	}
}
