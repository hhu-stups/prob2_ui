package de.prob2.ui.project;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

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
	
	public String getFileName() {
		String pattern = Pattern.quote(System.getProperty("file.separator"));
		String[] splittedFileName = location.split(pattern);
		return splittedFileName[splittedFileName.length-1];
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
