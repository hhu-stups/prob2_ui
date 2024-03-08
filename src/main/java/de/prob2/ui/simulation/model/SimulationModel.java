package de.prob2.ui.simulation.model;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSetter;

import de.prob2.ui.simulation.table.SimulationItem;

import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;

@JsonPropertyOrder({ "path", "simulationItems" })
public final class SimulationModel {

	private final ObjectProperty<Path> path;
	private final ListProperty<SimulationItem> simulationItems;

	@JsonCreator
	public SimulationModel(
		@JsonProperty("path") Path path,
		@JsonProperty("simulationItems") List<SimulationItem> simulationItems
	) {
		this();
		this.setPath(path);
		this.setSimulationItems(simulationItems);
	}

	public SimulationModel() {
		this.path = new SimpleObjectProperty<>(this, "path", null);
		this.simulationItems = new SimpleListProperty<>(this, "simulationItems", FXCollections.observableArrayList());
	}

	public ObjectProperty<Path> pathProperty() {
		return this.path;
	}

	@JsonGetter("path")
	public Path getPath() {
		return this.pathProperty().get();
	}

	@JsonSetter("path")
	private void setPath(Path path) {
		this.pathProperty().set(path);
	}

	public ListProperty<SimulationItem> simulationItemsProperty() {
		return this.simulationItems;
	}

	@JsonGetter("simulationItems")
	public List<SimulationItem> getSimulationItems() {
		return this.simulationItemsProperty().get();
	}

	@JsonSetter("simulationItems")
	private void setSimulationItems(List<SimulationItem> simulationItems) {
		this.simulationItemsProperty().setAll(simulationItems);
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
		return Objects.hash(this.getPath());
	}

	@Override
	public String toString() {
		return Objects.toString(this.getPath(), "<null>");
	}
}
