package de.prob2.ui.simulation.configuration;

import java.util.Objects;

public abstract class DiagramConfiguration {

	protected String id;

	public DiagramConfiguration(String id) {
		this.id = Objects.requireNonNull(id, "id");
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = Objects.requireNonNull(id, "id");
	}
}
