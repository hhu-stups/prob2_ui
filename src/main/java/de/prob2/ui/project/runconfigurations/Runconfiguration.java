package de.prob2.ui.project.runconfigurations;

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
		if ("default".equals(this.preference)) {
			return this.machine;
		}
		return this.machine + "." + this.preference;
	}
	
	@Override
	public boolean equals(Object object) {
		if (object == null) {
			return false;
		}
		if (!(object instanceof Runconfiguration)) {
			return false;
		}
		final Runconfiguration runconfig = (Runconfiguration) object;
		if (this.machine == null || runconfig.machine == null || !this.machine.equals(runconfig.machine)) {
			return false;
		}
		if (this.preference == null || runconfig.preference == null || !this.preference.equals(runconfig.preference) ) {
			return false;
		}
		return true;
	}
	
	@Override
	public int hashCode() {
		return this.machine.hashCode() + this.preference.hashCode();
	}
}
