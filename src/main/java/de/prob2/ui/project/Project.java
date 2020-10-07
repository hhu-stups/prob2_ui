package de.prob2.ui.project;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import de.prob.json.JsonManager;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.project.preferences.Preference;

import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

public class Project {
	public static final JsonDeserializer<Project> JSON_DESERIALIZER = Project::new;
	
	private String name;
	private String description;
	private List<Machine> machines;
	private List<Preference> preferences;
	private transient Path location;

	public Project(String name, String description, List<Machine> machines, List<Preference> preferences, Path location) {
		this.name = name;
		this.description = description;
		this.machines = new ArrayList<>(machines);
		this.preferences = new ArrayList<>(preferences);
		this.location = location;
	}
	
	private Project(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) {
		final JsonObject object = json.getAsJsonObject();
		this.name = JsonManager.checkDeserialize(context, object, "name", String.class);
		this.description = JsonManager.checkDeserialize(context, object, "description", String.class);
		this.machines = JsonManager.checkDeserialize(context, object, "machines", new TypeToken<List<Machine>>() {}.getType());
		this.preferences = JsonManager.checkDeserialize(context, object, "preferences", new TypeToken<List<Preference>>() {}.getType());
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
