package de.prob2.ui.simulation.configuration;

import java.util.List;

public class SimulationConfiguration {

	private final List<ActivationConfiguration> activations;

	public SimulationConfiguration(List<ActivationConfiguration> activations) {
		this.activations = activations;
	}

	public List<ActivationConfiguration> getActivationConfigurations() {
		return activations;
	}

}
