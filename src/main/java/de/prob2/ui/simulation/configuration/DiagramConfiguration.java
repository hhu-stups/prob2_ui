package de.prob2.ui.simulation.configuration;

public abstract class DiagramConfiguration {

	protected String id;

	public DiagramConfiguration(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
}
