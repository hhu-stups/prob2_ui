package de.prob2.ui.simulation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import de.prob2.ui.simulation.table.SimulationItem;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;

import java.nio.file.Path;
import java.util.List;

@JsonPropertyOrder({
		"path",
		"simulationItems"
})
public class SimulationModel {

	private final ObjectProperty<Path> path;

	private final ListProperty<SimulationItem> simulationItems;

	@JsonCreator
	public SimulationModel(
			@JsonProperty("path") final Path path,
			@JsonProperty("simulationItems") final List<SimulationItem> simulationItems
	) {
		this();
		this.path.set(path);
		this.simulationItems.clear();
		this.simulationItems.addAll(simulationItems);
	}

	public SimulationModel() {
		this.path = new SimpleObjectProperty<>(this, "path", null);
		this.simulationItems = new SimpleListProperty<>(this, "simulationItems", FXCollections.observableArrayList());
	}

	public ObjectProperty<Path> pathProperty() {
		return path;
	}

	@JsonProperty("path")
	public Path getPath() {
		return path.get();
	}

	@JsonProperty
	public void setPath(Path path) {
		this.path.set(path);
	}


	public ListProperty<SimulationItem> simulationItemsProperty() {
		return simulationItems;
	}

	@JsonProperty("simulationItems")
	public List<SimulationItem> getSimulationItems() {
		return simulationItems.get();
	}

	@JsonProperty
	private void setSimulationItems(final List<SimulationItem> simulationItems) {
		this.simulationItemsProperty().setAll(simulationItems);
	}

	public void reset() {
		for(SimulationItem item : this.simulationItems) {
			item.reset();
		}
	}

	@Override
	public String toString() {
		if(path.get() == null) {
			return "";
		}
		return path.get().toString();
	}
}
