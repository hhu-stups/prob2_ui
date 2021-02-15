package de.prob2.ui.simulation.table;

import java.util.List;

public class SimulationChoiceDebugItem extends SimulationDebugItem {

	private final List<String> activations;

	private final List<String> probability;

	public SimulationChoiceDebugItem(String id, List<String> activations, List<String> probability) {
		super(id);
		this.activations = activations;
		this.probability = probability;
	}


	public List<String> getActivations() {
		return activations;
	}

	public String getActivationsAsString() {
		return activations.toString();
	}

	public List<String> getProbability() {
		return probability;
	}

	public String getProbabilityAsString() {
		return probability.toString();
	}
}
