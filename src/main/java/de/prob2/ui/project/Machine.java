package de.prob2.ui.project;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Machine {
	private String name;
	private String description;
	private List<String> preferences;
	private Path location;
	
	public Machine(String name, String description, List<String> preferences, Path location) {
		this.name = name;
		this.description = description;
		this.preferences = preferences;
		this.location = location;
	}
	
	public Machine(String name, String description, Path location) {
		this.name = name;
		this.description = description;
		this.location = location;
		this.preferences = new ArrayList<>();
	}
	
	public String getName() {
		return name;
	}
	
	public String getDescription() {
		return description;
	}
	
	public List<String> getPreferences() {
		return preferences;
	}
	
	public Path getPath() {
		return location;
	}
	
	@Override
	public String toString() {
		return this.name;
	}
}
