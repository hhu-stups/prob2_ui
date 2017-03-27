package de.prob2.ui.project;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
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

	public Project(String name, String description, Machine machine, File location) {
		this.name = name;
		this.description = description;
		machines.add(machine);
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

	public void setLocation(File location) {
		this.location = location;
	}

	@Override
	public boolean equals(Object other) {
		if (other == this) {
			return true;
		}
		if (!(other instanceof Project)) {
			return false;
		}
		Project otherProject = (Project) other;

		if(otherProject.name.equals(this.name) &&
				otherProject.description.equals(this.description) &&
				otherProject.machines.equals(this.machines) &&
				otherProject.preferences.equals(this.preferences) &&
				otherProject.runconfigurations.equals(this.runconfigurations) &&
				otherProject.location.equals(this.location)) {
			return true;
		}
		return false;
	}

	@Override
	public int hashCode() {
		 return Objects.hash(name, description, machines, preferences, runconfigurations, location);
	}
}
