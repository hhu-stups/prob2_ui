package de.prob2.ui.project;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

import de.prob.json.HasMetadata;
import de.prob.json.JsonMetadata;
import de.prob.json.JsonMetadataBuilder;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.project.preferences.Preference;
import de.prob2.ui.vomanager.Requirement;

public class Project implements HasMetadata {
	public static final String FILE_TYPE = "Project";
	public static final int CURRENT_FORMAT_VERSION = 57;
	
	private final String name;
	private final String description;
	private final List<Machine> machines;
	private final List<Requirement> requirements;
	private final List<Preference> preferences;
	private final JsonMetadata metadata;
	@JsonIgnore
	private Path location;

	public Project(
		String name,
		String description,
		List<Machine> machines,
		List<Requirement> requirements,
		List<Preference> preferences,
		JsonMetadata metadata,
		Path location
	) {
		this.name = Objects.requireNonNull(name, "name");
		this.description = Objects.requireNonNull(description, "description");
		this.machines = new ArrayList<>(machines);
		this.requirements = new ArrayList<>(requirements);
		this.preferences = new ArrayList<>(preferences);
		this.metadata = metadata;
		this.location = location;
	}

	@JsonCreator
	public Project(
		@JsonProperty("name") String name,
		@JsonProperty("description") String description,
		@JsonProperty("machines") List<Machine> machines,
		@JsonProperty("requirements") List<Requirement> requirements,
		@JsonProperty("preferences") List<Preference> preferences,
		@JsonProperty("metadata") JsonMetadata metadata
	) {
		this(name, description, machines, requirements, preferences, metadata, null);
	}
	
	public static JsonMetadataBuilder metadataBuilder() {
		return new JsonMetadataBuilder(FILE_TYPE, CURRENT_FORMAT_VERSION)
			.withUserCreator()
			.withSavedNow();
	}

	public String getName() {
		return name;
	}
	
	public String getDescription() {
		return description;
	}
	
	public List<Machine> getMachines() {
		return Collections.unmodifiableList(machines);
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
		return Collections.unmodifiableList(preferences);
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

	public List<Requirement> getRequirements() {
		return Collections.unmodifiableList(requirements);
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
			machine.setChanged(false);
		}
	}
	
	@Override
	public boolean equals(Object other) {
		if (other == this) {
			return true;
		}
		if (!(other instanceof Project otherProject)) {
			return false;
		}

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

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
			       .add("name", this.getName())
			       .add("location", this.getLocation())
			       .toString();
	}
}
