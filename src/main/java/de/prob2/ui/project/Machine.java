package de.prob2.ui.project;

import java.io.File;

public class Machine {
	private String name;
	private String description;
	private File location;
	
	public Machine(String name, String description, File location) {
		this.name = name;
		this.description = description;
		this.location = location;
	}
	
	public String getName() {
		return name;
	}
}
