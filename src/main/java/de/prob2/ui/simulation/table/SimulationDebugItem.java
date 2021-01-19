package de.prob2.ui.simulation.table;

import java.util.List;
import java.util.Map;

public class SimulationDebugItem {

	private String opName;

	private List<Map<String, Integer>> activation;

	private String priority;

	private List<String> probability;

	private List<Map<String, Object>> values;

	public SimulationDebugItem(String opName, List<Map<String, Integer>> activation, String priority, List<String> probability,
							   List<Map<String, Object>> values) {
		this.opName = opName;
		this.activation = activation;
		this.priority = priority;
		this.probability = probability;
		this.values = values;
	}

	public String getOpName() {
		return opName;
	}

	public List<Map<String, Integer>> getActivation() {
		return activation;
	}

	public String getActivationAsString() {
		return activation == null ? "" : activation.toString();
	}

	public String getPriority() {
		return priority;
	}

	public List<String> getProbability() {
		return probability;
	}

	public String getProbabilityAsString() {
		return probability == null ? "" : probability.toString();
	}

	public List<Map<String, Object>> getValues() {
		return values;
	}

	public String getValuesAsString() {
		return values == null ? "" : values.toString();
	}
}
