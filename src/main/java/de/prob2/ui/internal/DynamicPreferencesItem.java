package de.prob2.ui.internal;

import de.prob2.ui.preferences.ProBPreferenceType;

public class DynamicPreferencesItem {

	private final String name;
	
	private String changed;
	
	private String value;
	
	private String prefValue;
	
	private final String description;
	
	private final ProBPreferenceType valueType;
	
	public DynamicPreferencesItem(final String name, final String prefValue, final String description, final ProBPreferenceType valueType) {
		this.name = name;
		this.prefValue = prefValue;
		this.description = description;
		this.valueType = valueType;
	}
	
	public String getName() {
		return name;
	}
	 
	public String getDescription() {
		return description;
	}
	
	public String getValue() {
		return value;
	}
	
	public String getChanged() {
		return changed;
	}
	
	public String getPrefValue() {
		return prefValue;
	}
	
	public ProBPreferenceType getValueType() {
		return valueType;
	}
	
	public void setValue(String value) {
		this.value = value;
	}
	
	public void setChanged(String changed) {
		this.changed = changed;
	}

}
