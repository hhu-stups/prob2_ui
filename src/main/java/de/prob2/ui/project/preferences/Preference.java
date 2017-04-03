package de.prob2.ui.project.preferences;

import java.util.Map;

public class Preference {

	private String name;
	private Map<String, String> preferences;

	public Preference(String name, Map<String, String> preferences) {
		this.name = name;
		this.preferences = preferences;
	}
	
	@Override
	public String toString() {
		return this.name;
	}
	
	public Map<String, String> getPreferences() {
		return preferences;
	}

	public String getName() {
		return name;
	}
}
