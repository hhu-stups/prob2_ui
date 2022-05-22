package de.prob2.ui.project;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import de.prob.json.HasMetadata;
import de.prob.json.JsonMetadata;
import de.prob.json.JsonMetadataBuilder;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.project.preferences.Preference;
import de.prob2.ui.vomanager.Requirement;

import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;

public class Project implements HasMetadata {
	public static final String FILE_TYPE = "Project";
	public static final int CURRENT_FORMAT_VERSION = 29;
	
	private String name;
	private String description;
	private List<Machine> machines;
	private final ListProperty<Requirement> requirements;
	private List<Preference> preferences;
	private JsonMetadata metadata;
	@JsonIgnore
	private Path location;

	@JsonCreator
	public Project(
		@JsonProperty("name") final String name,
		@JsonProperty("description") final String description,
		@JsonProperty("machines") final List<Machine> machines,
		@JsonProperty("requirements") final List<Requirement> requirements,
		@JsonProperty("preferences") final List<Preference> preferences,
		@JsonProperty("metadata") final JsonMetadata metadata,
		@JsonProperty("location") final Path location
	) {
		this.name = name;
		this.description = description;
		this.machines = new ArrayList<>(machines);
		this.requirements = new SimpleListProperty<>(this, "requirements", FXCollections.observableArrayList());
		this.requirements.setAll(requirements);
		this.preferences = new ArrayList<>(preferences);
		this.metadata = metadata;
		this.location = location;
	}
	
	public static JsonMetadataBuilder metadataBuilder() {
		return new JsonMetadataBuilder(FILE_TYPE, CURRENT_FORMAT_VERSION)
			.withUserCreator()
			.withSavedNow();
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
	
	public Path getAbsoluteMachinePath(final Machine machine) {
		if (!this.getMachines().contains(machine)) {
			throw new IllegalArgumentException("Machine " + machine + " is not part of project " + this);
		}
		//Normalize resulting path to get rid of .. and .
		return this.getLocation().resolve(machine.getLocation()).normalize();
	}
	
	public List<Preference> getPreferences() {
		return preferences;
	}

	public void setPreferences(List<Preference> preferences) {
		this.preferences = preferences;
	}
	
	public Preference getPreference(final String name) {
		if (Preference.DEFAULT.getName().equals(name)) {
			return Preference.DEFAULT;
		}
		for (final Preference pref : this.getPreferences()) {
			if (pref.getName().equals(name)) {
				return pref;
			}
		}
		throw new NoSuchElementException("Could not find preference with name " + name);
	}

	public ListProperty<Requirement> requirementsProperty() {
		return requirements;
	}

	public List<Requirement> getRequirements() {
		return requirements.get();
	}

	public void setRequirements(List<Requirement> requirements) {
		this.requirements.setAll(requirements);
	}

	@Override
	public JsonMetadata getMetadata() {
		return this.metadata;
	}
	
	@Override
	public Project withMetadata(final JsonMetadata metadata) {
		return new Project(
			this.getName(),
			this.getDescription(),
			this.getMachines(),
			this.getRequirements(),
			this.getPreferences(),
			metadata,
			this.getLocation()
		);
	}
	
	@JsonIgnore
	public Path getLocation() {
		return location;
	}
	
	@JsonIgnore
	void setLocation(Path location) {
		this.location = location;
	}
	
	public void resetChanged() {
		for (Machine machine : this.getMachines()) {
			machine.changedProperty().set(false);
		}
		for (Preference pref : this.getPreferences()) {
			pref.changedProperty().set(false);
		}
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
				otherProject.requirements.equals(this.requirements) &&
				otherProject.preferences.equals(this.preferences) &&
				otherProject.metadata.equals(this.metadata) &&
				otherProject.location.equals(this.location);
	}

	@Override
	public int hashCode() {
		 return Objects.hash(name, description, machines, requirements, preferences, metadata, location);
	}
}
