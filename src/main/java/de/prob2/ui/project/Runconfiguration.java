package de.prob2.ui.project;

public class Runconfiguration {
	private String machine;
	private String preference;

	public Runconfiguration(String machine, String preference) {
		this.machine = machine;
		this.preference = preference;
	}

	public String getMachine() {
		return machine;
	}

	public String getPreference() {
		return preference;
	}
	
	@Override
	public String toString() {
		if(this.preference.equals("default")) 
			return this.machine;
		return this.machine + "." + this.preference;
	}
}
