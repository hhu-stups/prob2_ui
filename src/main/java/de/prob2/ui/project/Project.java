package de.prob2.ui.project;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.project.preferences.Preference;

public class Project {
	private String name;
	private String description;
	private List<Machine> machines = new ArrayList<>();
	private List<Preference> preferences = new ArrayList<>();
	private transient Path location;

	public Project(String name, String description, List<Machine> machines, List<Preference> preferences, Path location) {
		this.name = name;
		this.description = description;
		this.machines.addAll(machines);
		this.preferences.addAll(preferences);
		this.location = location;
	}

	public Project(String name, String description, Path location) {
		this.name = name;
		this.description = description;
		this.location = location;
	}

	public Project(String name, String description, Machine machine, Path location) {
		this.name = name;
		this.description = description;
		this.machines.add(machine);
		this.location = location;
	}

	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getDescription() {
		return description;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}

	public List<Machine> getMachines() {
		return machines;
	}
	
	public void setMachines(List<Machine> machines) {
		this.machines = machines;
	}
	
	public Machine getMachine(final String name) {
		for (final Machine machine : this.getMachines()) {
			if (machine.getName().equals(name)) {
				return machine;
			}
		}
		return null;
	}
	
	public List<Preference> getPreferences() {
		return preferences;
	}

	public void setPreferences(List<Preference> preferences) {
		this.preferences = preferences;
	}
	
	public Preference getPreference(final String name) {
		for (final Preference pref : this.getPreferences()) {
			if (pref.getName().equals(name)) {
				return pref;
			}
		}
		return null;
	}
	
	public Path getLocation() {
		return location;
	}
	
	void setLocation(Path location) {
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

		return otherProject.name.equals(this.name) &&
				otherProject.description.equals(this.description) &&
				otherProject.machines.equals(this.machines) &&
				otherProject.preferences.equals(this.preferences) &&
				otherProject.location.equals(this.location);
	}

	@Override
	public int hashCode() {
		 return Objects.hash(name, description, machines, preferences, location);
	}
}
