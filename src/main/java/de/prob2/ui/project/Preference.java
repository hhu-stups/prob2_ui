package de.prob2.ui.project;

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
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Preference other = (Preference) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
	
	public Map<String, String> getPreferences() {
		return preferences;
	}
}
