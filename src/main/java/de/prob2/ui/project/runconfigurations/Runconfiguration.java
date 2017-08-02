package de.prob2.ui.project.runconfigurations;

import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.project.preferences.DefaultPreference;
import de.prob2.ui.project.preferences.Preference;

import java.util.Objects;

public class Runconfiguration {
	private String name;
	private transient Machine machine;
	private transient Preference preference;

	public Runconfiguration(Machine machine, Preference preference) {
		this.machine = machine;
		this.preference = preference;
		this.name = this.machine.getName() + "." + this.preference.getName();
	}

	public String getName() {
		if (machine != null && preference != null) {
			this.name = this.machine.getName() + "." + this.preference.getName();
		}
		return this.name;
	}

	public Machine getMachine() {
		return machine;
	}

	public Preference getPreference() {
		return preference;
	}

	public String getMachineName() {
		return this.getName().split("\\.")[0];
	}

	public String getPreferenceName() {
		return this.getName().split("\\.")[1];
	}

	@Override
	public String toString() {
		if (this.preference instanceof DefaultPreference) {
			return this.machine.getName();
		}
		return this.getName();
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
		if (!this.getName().equals(runconfig.getName())) {
			return false;
		}
		if (this.machine == null || runconfig.machine == null || !this.machine.equals(runconfig.machine)) {
			return false;
		}
		return !(this.preference == null || runconfig.preference == null
				|| !this.preference.equals(runconfig.preference));
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.getName());
	}
}
