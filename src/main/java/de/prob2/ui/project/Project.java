package de.prob2.ui.project;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.project.preferences.Preference;
import de.prob2.ui.project.runconfigurations.Runconfiguration;
import de.prob2.ui.verifications.ltl.LTLFormulaItem;

public class Project {
	private final String name;
	private final String description;
	private final List<Machine> machines = new ArrayList<>();
	private final List<Preference> preferences = new ArrayList<>();
	private final Set<Runconfiguration> runconfigurations = new HashSet<>();
	private final List<LTLFormulaItem> ltlFormulas = new ArrayList<>();
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
			List<Runconfiguration> runconfigurations, List<LTLFormulaItem> ltlFormulas, File location) {
		this.name = name;
		this.description = description;
		this.machines.addAll(machines);
		this.preferences.addAll(preferences);
		this.runconfigurations.addAll(runconfigurations);
		this.ltlFormulas.addAll(ltlFormulas);
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
	
	public Project(String name, String description, List<Machine> machines, List<Preference> preferences,
			Set<Runconfiguration> runconfigurations, List<LTLFormulaItem> ltlFormulas, File location) {
		this.name = name;
		this.description = description;
		this.machines.addAll(machines);
		this.preferences.addAll(preferences);
		this.runconfigurations.addAll(runconfigurations);
		this.ltlFormulas.addAll(ltlFormulas);
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
	
	public List<LTLFormulaItem> getLtLFormulas() {
		return ltlFormulas;
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

		return otherProject.name.equals(this.name) &&
				otherProject.description.equals(this.description) &&
				otherProject.machines.equals(this.machines) &&
				otherProject.preferences.equals(this.preferences) &&
				otherProject.runconfigurations.equals(this.runconfigurations) &&
				otherProject.location.equals(this.location) &&
				otherProject.ltlFormulas.equals(this.ltlFormulas);
	}

	@Override
	public int hashCode() {
		 return Objects.hash(name, description, machines, preferences, runconfigurations, ltlFormulas ,location);
	}
}
