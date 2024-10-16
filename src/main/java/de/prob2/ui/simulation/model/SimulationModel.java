package de.prob2.ui.simulation.model;

import java.nio.file.Path;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({ "path" })
public final class SimulationModel {

	private final Path path;

	@JsonCreator
	public SimulationModel(@JsonProperty("path") Path path) {
		this.path = Objects.requireNonNull(path, "path");
	}

	public Path getPath() {
		return this.path;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		} else if (!(o instanceof SimulationModel that)) {
			return false;
		} else {
			return Objects.equals(this.getPath(), that.getPath());
		}
	}

	@Override
	public int hashCode() {
		return this.getPath().hashCode();
	}

	@Override
	public String toString() {
		return path.toString().isEmpty() ? "Default Simulation" : path.toString();
	}
}
