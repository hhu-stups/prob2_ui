package de.prob2.ui.project;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public class MachineTableItem {
	private Machine machine;
	private String name;
	private String description;
	private Map<Preference, BooleanProperty> preferences = new HashMap<>();
	
	public MachineTableItem(Machine machine, List<Preference> preferenceList) {
		this.machine = machine;
		this.name = machine.getName();
		this.description = machine.getDescription();
		
		for(Preference p : preferenceList) {
			preferences.put(p, new SimpleBooleanProperty(false));
		}
	}
	
	public BooleanProperty getPreferenceProperty(Preference p) {
		return preferences.get(p);
	}

	public Machine get() {
		return machine;
	}
	
	public void addPreferenceProperty(Preference p) {
		preferences.put(p, new SimpleBooleanProperty(false));
	}
	
	public String getName() {
		return name;
	}
	
	public String getDescription() {
		return description;
	}
	
	public Map<Preference, BooleanProperty> getPreferences() {
		return preferences;
	}
}
