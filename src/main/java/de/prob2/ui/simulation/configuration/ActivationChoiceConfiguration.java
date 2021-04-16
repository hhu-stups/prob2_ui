package de.prob2.ui.simulation.configuration;

import java.util.Map;

public class ActivationChoiceConfiguration extends ActivationConfiguration {

	private final Map<String, String> activations;

	public ActivationChoiceConfiguration(String id, Map<String, String> activations) {
		super(id);
		this.activations = activations;
	}

	public Map<String, String> getActivations() {
		return activations;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("ActivationChoiceActivation(");
		sb.append("id");
		sb.append("=");
		sb.append(id);
		sb.append(", ");
		sb.append("activations");
		sb.append("=");
		sb.append(activations);
		sb.append(")");
		return sb.toString();
	}
}
