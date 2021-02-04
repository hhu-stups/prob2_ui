package de.prob2.ui.simulation.table;

import de.prob.statespace.Trace;
import de.prob2.ui.simulation.SimulationCheckingConfiguration;
import de.prob2.ui.simulation.choice.SimulationType;
import de.prob2.ui.verifications.Checked;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SimulationItem {

    private SimulationType type;

    private String configuration;

    private SimulationCheckingConfiguration simulationCheckingConfiguration;

    private final transient ObjectProperty<Checked> checked = new SimpleObjectProperty<>(this, "checked", Checked.NOT_CHECKED);

    private transient ListProperty<Trace> traces;

    private transient ListProperty<List<Integer>> timestamps;

    public SimulationItem(SimulationCheckingConfiguration simulationCheckingConfiguration) {
        this.simulationCheckingConfiguration = simulationCheckingConfiguration;
        this.traces = new SimpleListProperty<>(FXCollections.observableArrayList());
        this.timestamps = new SimpleListProperty<>(FXCollections.observableArrayList());
        updateItem();
    }

    private void updateItem() {
        this.type = simulationCheckingConfiguration.getType();
        this.configuration = simulationCheckingConfiguration.getConfiguration();
    }

    public void setChecked(Checked checked) {
        this.checked.set(checked);
    }

    public ObjectProperty<Checked> checkedProperty() {
        return checked;
    }

    public Checked getChecked() {
        return checked.get();
    }

    public String getTypeAsName() {
        return type.getName();
    }

    public SimulationType getType() {
        return type;
    }

    public SimulationCheckingConfiguration getSimulationConfiguration() {
        return simulationCheckingConfiguration;
    }

    public String getConfiguration() {
        return configuration;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, configuration);
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof SimulationItem)) {
            return false;
        }
        SimulationItem otherItem = (SimulationItem) obj;
        return this.configuration.equals(otherItem.getConfiguration()) && this.type.equals(otherItem.getType());
    }

    public void reset() {
        // TODO
    }

    public void setTraces(List<Trace> traces) {
        this.traces.setAll(traces);
    }

    public ListProperty<Trace> tracesProperty() {
        return traces;
    }

    public List<Trace> getTraces() {
        return traces.get();
    }

	public void setTimestamps(List<List<Integer>> timestamps) {
		this.timestamps.setAll(timestamps);
	}

	public ListProperty<List<Integer>> timestampsProperty() {
		return timestamps;
	}

	public List<List<Integer>> getTimestamps() {
		return timestamps.get();
	}
}
