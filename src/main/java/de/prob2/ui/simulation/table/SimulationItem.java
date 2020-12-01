package de.prob2.ui.simulation.table;

import java.util.Map;

public class SimulationItem {

	private String opName;

	private String time;

	private String delay;

	private String priority;

	private String probability;

	private String choiceID;

	private Map<String, String> values;

	private String valuesProbability;

	public SimulationItem(String opName, String time, String delay, String priority, String probability,
						  String choiceID, Map<String, String> values, String valuesProbability) {
		this.opName = opName;
		this.time = time;
		this.delay = delay;
		this.priority = priority;
		this.probability = probability;
		this.choiceID = choiceID;
		this.values = values;
		this.valuesProbability = valuesProbability;
	}

	public String getOpName() {
		return opName;
	}

	public String getTime() {
		return time;
	}

	public String getDelay() {
		return delay;
	}

	public String getPriority() {
		return priority;
	}

	public String getProbability() {
		return probability;
	}

	public String getChoiceID() {
		return choiceID;
	}

	public Map<String, String> getValues() {
		return values;
	}

	public String getValuesAsString() {
		return values == null ? "" : values.toString();
	}

	public String getValuesProbability() {
		return valuesProbability;
	}
}
