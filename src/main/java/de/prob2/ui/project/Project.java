package de.prob2.ui.project;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Project {
	private String name;
	private String description;
	private File location;
	private List<Machine> machines = new ArrayList<Machine>();
	private Map<String, Preference> preferences = new HashMap<>();

	private final boolean singleFile;

	public Project(String name, String description, List<Machine> machines, Map<String, Preference> preferences,
			File location) {
		this.name = name;
		this.description = description;
		this.machines = machines;
		this.singleFile = false;
		this.location = location;
		this.preferences = preferences;
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
}
