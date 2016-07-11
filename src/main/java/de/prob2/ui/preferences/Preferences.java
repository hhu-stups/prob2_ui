package de.prob2.ui.preferences;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.prob.animator.command.GetDefaultPreferencesCommand;
import de.prob.animator.command.GetPreferenceCommand;
import de.prob.animator.command.SetPreferenceCommand;
import de.prob.animator.domainobjects.ProBPreference;
import de.prob.statespace.StateSpace;

public class Preferences {
	private StateSpace stateSpace;

	public Preferences(StateSpace stateSpace) {
		this.stateSpace = stateSpace;
	}

	public List<ProBPreference> getPreferences() {
		GetDefaultPreferencesCommand cmd = new GetDefaultPreferencesCommand();
		this.stateSpace.execute(cmd);
		return cmd.getPreferences();
	}

	public String getPreferenceValue(String name) {
		GetPreferenceCommand cmd = new GetPreferenceCommand(name);
		this.stateSpace.execute(cmd);
		return cmd.getValue();
	}

	public void setPreferenceValue(String name, String value) {
		this.stateSpace.execute(new SetPreferenceCommand(name, value));
	}
}
