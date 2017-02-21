package de.prob2.ui.project;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Project {
	private final String name;
	private final String description;
	private final List<Machine> machines = new ArrayList<>();
	private final List<Preference> preferences = new ArrayList<>();
	private final Set<Runconfiguration> runconfigurations = new HashSet<>();
	private transient File location;

	public Project(String name, String description, List<Machine> machines, List<Preference> preferences,
			List<Runconfiguration> runconfigurations, File location) {
		this.name = name;
		this.description = description;
		this.machines.addAll(machines);
		this.preferences.addAll(preferences);
		this.runconfigurations.addAll(runconfigurations);
		this.location = location;
	}
	
	public Project(String name, String description, List<Machine> machines, List<Preference> preferences,
			Set<Runconfiguration> runconfigurations, File location) {
		this.name = name;
		this.description = description;
		this.machines.addAll(machines);
		this.preferences.addAll(preferences);
		this.runconfigurations.addAll(runconfigurations);
		this.location = location;
	}

	public Project(String name, String description, File location) {
		this.name = name;
		this.description = description;
		this.location = location;
	}

	public String getName() {
		return name;
	}

	public List<Machine> getMachines() {
		return machines;
	}

	public File getLocation() {
		return location;
	}

	public String getDescription() {
		return description;
	}

	public List<Preference> getPreferences() {
		return preferences;
	}

	public Set<Runconfiguration> getRunconfigurations() {
		return runconfigurations;
	}

	public Map<String, String> getPreferences(Machine machine) {
		List<String> prefNames = machine.getPreferences();
		Map<String, String> prefs = new HashMap<>();
		if (!prefNames.isEmpty()) {
			for (Preference pref : preferences) {
				String n = pref.getName();
				if (prefNames.contains(n)) {
					prefs.putAll(pref.getPreferences());
				}
			}
		}
		return prefs;
	}

	public void setLocation(File location) {
		this.location = location;
	}
}
