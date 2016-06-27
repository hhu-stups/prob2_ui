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

	public Map<String, Map<String, ProBPreference>> getPreferencesTree() {
		Map<String, Map<String, ProBPreference>> tree = new HashMap<>();

		for (ProBPreference pref : this.getPreferences()) {
			if (!tree.containsKey(pref.category)) {
				tree.put(pref.category, new HashMap<>());
			}

			tree.get(pref.category).put(pref.name, pref);
		}

		return tree;
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
