package de.prob2.ui.project;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Machine {
	private String name;
	private String description;
	private List<String> preferences;
	private File location;
	
	public Machine(String name, String description, List<String> preferences, File location) {
		this.name = name;
		this.description = description;
		this.preferences = preferences;
		this.location = location;
	}
	
	public Machine(String name, String description, File location) {
		this.name = name;
		this.description = description;
		this.location = location;
		this.preferences = new ArrayList<>();
	}
	
	public String getName() {
		return name;
	}
	
	public File getLocation() {
		return location;
	}
	
	public String getDescription() {
		return description;
	}
	
	@Override
	public String toString() {
		return this.name;
	}

	public String getPath() {
		return location.getPath();
	}
	
	public List<String> getPreferences() {
		return preferences;
	}
}
