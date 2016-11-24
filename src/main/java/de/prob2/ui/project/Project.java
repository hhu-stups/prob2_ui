package de.prob2.ui.project;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Project {
	private String name;
	private String description;
	private File location;
	private List<Machine> machines = new ArrayList<Machine>();

	private final boolean singleFile;

	public Project(String name, String description, List<Machine> machines, File location) {
		this.name = name;
		this.description = description;
		this.machines = machines;
		this.singleFile = false;
		this.location = location;
	}

	public Project(File file) {
		this.name = file.getName();
		this.machines = new ArrayList<Machine>();
		String name[] = file.getName().split("\\.");
		machines.add(new Machine(name[0], "", file));
		this.singleFile = true;
	}

	public String getName() {
		return name;
	}

	public List<Machine> getMachines() {
		return machines;
	}

	public boolean isSingleFile() {
		return singleFile;
	}

	public void addMachine(File machine) {
		String name[] = machine.getName().split("\\.");
		machines.add(new Machine(name[0], "", machine));
	}

	public File getLocation() {
		return location;
	}

	public String getDescription() {
		return description;
	}
}
