package de.prob2.ui.simulation.configuration;

import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ActivationChoiceConfiguration extends DiagramConfiguration {

	@JsonProperty("chooseActivation")
	private Map<String, String> activations;

	public ActivationChoiceConfiguration(String id, Map<String, String> activations) {
		super(id);
		this.activations = Objects.requireNonNull(activations, "activations");
	}

	public void setActivations(Map<String, String> activations) {
		this.activations = Objects.requireNonNull(activations, "activations");
	}

	public Map<String, String> getActivations() {
		return activations;
	}

	public String getActivationsAsString() {
		return activations.toString();
	}

	@Override
	public String toString() {
		return "ActivationChoiceActivation(id=" + id + ", activations=" + activations + ")";
	}
}
