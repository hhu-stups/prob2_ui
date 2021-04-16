package de.prob2.ui.simulation.configuration;

public abstract class ActivationConfiguration {

	protected final String id;

	public ActivationConfiguration(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}
}
