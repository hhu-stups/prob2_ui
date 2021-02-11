package de.prob2.ui.simulation.table;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.prob.json.JsonManager;
import de.prob.statespace.Trace;
import de.prob2.ui.simulation.SimulationCheckingConfiguration;
import de.prob2.ui.simulation.choice.SimulationType;
import de.prob2.ui.simulation.simulators.check.SimulationStats;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.ltl.formula.LTLFormulaItem;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SimulationItem {
    public static final JsonDeserializer<SimulationItem> JSON_DESERIALIZER = SimulationItem::new;

    private SimulationCheckingConfiguration simulationCheckingConfiguration;

    private transient ObjectProperty<Checked> checked;

    private transient SimulationStats simulationStats;

    private transient ListProperty<Trace> traces;

    private transient ListProperty<List<Integer>> timestamps;

    public SimulationItem(SimulationCheckingConfiguration simulationCheckingConfiguration) {
        this.simulationCheckingConfiguration = simulationCheckingConfiguration;
        initListeners();
    }

    private SimulationItem(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) {
        final JsonObject object = json.getAsJsonObject();
        this.simulationCheckingConfiguration = JsonManager.checkDeserialize(context, object, "simulationCheckingConfiguration", SimulationCheckingConfiguration.class);
        initListeners();
    }

    private void initListeners() {
        this.checked = new SimpleObjectProperty<>(this, "checked", Checked.NOT_CHECKED);
        this.traces = new SimpleListProperty<>(FXCollections.observableArrayList());
        this.timestamps = new SimpleListProperty<>(FXCollections.observableArrayList());
        this.simulationStats = null;
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
        return simulationCheckingConfiguration.getType().getName();
    }

    public SimulationType getType() {
        return simulationCheckingConfiguration.getType();
    }

    public SimulationCheckingConfiguration getSimulationConfiguration() {
        return simulationCheckingConfiguration;
    }

    public String getConfiguration() {
        return simulationCheckingConfiguration.getConfiguration();
    }

    @Override
    public int hashCode() {
        return Objects.hash(simulationCheckingConfiguration);
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof SimulationItem)) {
            return false;
        }
        SimulationItem otherItem = (SimulationItem) obj;
        return this.simulationCheckingConfiguration.equals(otherItem.simulationCheckingConfiguration);
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

	public void setSimulationStats(SimulationStats simulationStats) {
        this.simulationStats = simulationStats;
    }

    public SimulationStats getSimulationStats() {
        return simulationStats;
    }
}
