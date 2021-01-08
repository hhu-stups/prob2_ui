package de.prob2.ui.simulation.table;

import java.util.List;
import java.util.Map;

public class SimulationDebugItem {

	private String opName;

	private String time;

	private List<Map<String, Integer>> delay;

	private String priority;

	private List<String> probability;

	private List<Map<String, Object>> values;

	public SimulationDebugItem(String opName, String time, List<Map<String, Integer>> delay, String priority, List<String> probability,
							   List<Map<String, Object>> values) {
		this.opName = opName;
		this.time = time;
		this.delay = delay;
		this.priority = priority;
		this.probability = probability;
		this.values = values;
	}

	public String getOpName() {
		return opName;
	}

	public String getTime() {
		return time;
	}

	public List<Map<String, Integer>> getDelay() {
		return delay;
	}

	public String getDelayAsString() {
		return delay == null ? "" : delay.toString();
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
