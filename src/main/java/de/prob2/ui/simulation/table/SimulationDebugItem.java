package de.prob2.ui.simulation.table;

import de.prob2.ui.simulation.configuration.ActivationConfiguration;
import de.prob2.ui.simulation.configuration.ActivationOperationConfiguration;
import de.prob2.ui.simulation.configuration.OperationConfiguration;

import java.util.List;
import java.util.Map;

public class SimulationDebugItem {

	private final String opName;

	private final List<ActivationConfiguration> activation;

	private final OperationConfiguration.ActivationKind activationKind;

	private final String additionalGuards;

	private final String priority;

	private final Map<String, String> values;

	public SimulationDebugItem(String opName, List<ActivationConfiguration> activation, OperationConfiguration.ActivationKind activationKind,
							   String additionalGuards, String priority, Map<String, String> values) {
		this.opName = opName;
		this.activation = activation;
		this.activationKind = activationKind;
		this.additionalGuards = additionalGuards;
		this.priority = priority;
		this.values = values;
	}

	public String getOpName() {
		return opName;
	}

	public List<ActivationConfiguration> getActivation() {
		return activation;
	}

	public String getActivationAsString() {
		return activation == null ? "" : activation.toString();
	}

	public OperationConfiguration.ActivationKind getActivationKind() {
		return activationKind;
	}

	public String getAdditionalGuards() {
		return additionalGuards;
	}

	public String getPriority() {
		return priority;
	}

	public Map<String, String> getValues() {
		return values;
	}

	public String getValuesAsString() {
		return values == null ? "" : values.toString();
	}
}
