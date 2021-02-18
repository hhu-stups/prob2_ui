package de.prob2.ui.simulation.table;

import java.util.Map;

public class SimulationChoiceDebugItem extends SimulationDebugItem {

	private final Map<String, String> activations;

	public SimulationChoiceDebugItem(String id, Map<String, String> activations) {
		super(id);
		this.activations = activations;
	}


	public Map<String, String> getActivations() {
		return activations;
	}

	public String getActivationsAsString() {
		return activations.toString();
	}

}
