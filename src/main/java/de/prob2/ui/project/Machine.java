package de.prob2.ui.project;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Machine {
	private String name;
	private String description;
	private List<String> preferences;
	private String location;
	
	public Machine(String name, String description, List<String> preferences, Path location) {
		this.name = name;
		this.description = description;
		this.preferences = preferences;
		this.location = location.toString();
	}
	
	public Machine(String name, String description, Path location) {
		this.name = name;
		this.description = description;
		this.location = location.toString();
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
		return Paths.get(location);
	}
	
	@Override
	public String toString() {
		return this.name;
	}
}
