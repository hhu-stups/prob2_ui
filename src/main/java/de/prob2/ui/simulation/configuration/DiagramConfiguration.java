package de.prob2.ui.simulation.configuration;

public abstract class DiagramConfiguration {

	protected final String id;

	public DiagramConfiguration(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}
}
