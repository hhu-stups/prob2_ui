package de.prob2.ui.project;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Project {
	private final String name;
	private final String description;
	private transient File location;
	private final boolean singleFile;
	private final List<Machine> machines;
	private final Map<String, Preference> preferences;

	public Project(String name, String description, List<Machine> machines, Map<String, Preference> preferences,
			File location) {
		this.name = name;
		this.description = description;
		this.singleFile = false;
		this.machines = machines;
		this.location = location;
		this.preferences = preferences;
	}

	public Project(File file) {
		this.name = file.getName();
		this.description = "";
		this.singleFile = true;
		this.location = null;
		this.machines = new ArrayList<>();
		machines.add(new Machine(file.getName().split("\\.")[0], "", file));
		this.preferences = new HashMap<>();
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
		machines.add(new Machine(machine.getName().split("\\.")[0], "", machine));
	}

	public File getLocation() {
		return location;
	}

	public String getDescription() {
		return description;
	}

	public Map<String, Preference> getPreferences() {
		return preferences;
	}

	public Map<String, String> getPreferences(Machine machine) {
		List<String> prefNames = machine.getPreferences();
		Map<String, String> prefs = new HashMap<>();
		if (!prefNames.isEmpty()) {
			for (String prefName : prefNames) {
				Preference pref = preferences.get(prefName);
				prefs.putAll(pref.getPreferences());
			}
		}
		return prefs;
	}

	public void setLocation(File location) {
		this.location = location;
	}
}
