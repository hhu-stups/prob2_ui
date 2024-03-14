package de.prob2.ui.simulation.model;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import de.prob2.ui.simulation.table.SimulationItem;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

@JsonPropertyOrder({ "path", "simulationItems" })
public final class SimulationModel {

	private final Path path;
	private final ObservableList<SimulationItem> simulationItems;

	@JsonCreator
	public SimulationModel(
		@JsonProperty("path") Path path,
		@JsonProperty("simulationItems") List<SimulationItem> simulationItems
	) {
		this.path = Objects.requireNonNull(path, "path");
		this.simulationItems = FXCollections.observableArrayList(simulationItems);
	}

	public Path getPath() {
		return this.path;
	}

	public ObservableList<SimulationItem> getSimulationItems() {
		return this.simulationItems;
	}

	public void reset() {
		for (SimulationItem item : this.getSimulationItems()) {
			item.reset();
		}
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
		return Objects.toString(this.getPath(), "<null>");
	}
}
