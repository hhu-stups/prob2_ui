package de.prob2.ui.project;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Project {
	private String name;
	private String description;
	private File location;
	private List<File> machines = new ArrayList<File>();

	private final boolean singleFile;

	public Project(String name, String description, List<File> machines, File location) {
		this.name = name;
		this.description = description;
		this.machines = machines;
		this.singleFile = false;
		this.location = location;
	}

	public Project(File file) {
		this.name = file.getName();
		this.machines = new ArrayList<File>();
		machines.add(file);
		this.singleFile = true;
	}

	public String getName() {
		return name;
	}

	public List<File> getMachines() {
		return machines;
	}

	public boolean isSingleFile() {
		return singleFile;
	}

	public void addMachine(File machine) {
		this.machines.add(machine);
	}

	public File getLocation() {
		return location;
	}

	public String getDescription() {
		return description;
	}
}
