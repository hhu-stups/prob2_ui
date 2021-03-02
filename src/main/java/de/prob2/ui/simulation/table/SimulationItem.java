package de.prob2.ui.simulation.table;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.prob.json.JsonManager;
import de.prob.statespace.Trace;
import de.prob2.ui.animation.tracereplay.ReplayTrace;
import de.prob2.ui.simulation.choice.SimulationCheckingType;
import de.prob2.ui.simulation.choice.SimulationType;
import de.prob2.ui.simulation.simulators.check.SimulationEstimator;
import de.prob2.ui.simulation.simulators.check.SimulationHypothesisChecker;
import de.prob2.ui.simulation.simulators.check.SimulationStats;
import de.prob2.ui.verifications.Checked;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SimulationItem {

    public static class SimulationCheckingInformation {

        public static final JsonDeserializer<SimulationCheckingInformation> JSON_DESERIALIZER = SimulationCheckingInformation::new;

        private final Map<String, Object> information;

        public SimulationCheckingInformation(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) {
            final JsonObject object = json.getAsJsonObject();
            information = new HashMap<>();
            if(object.has("PROBABILITY")) {
                information.put("PROBABILITY", JsonManager.checkDeserialize(context, object, "PROBABILITY", double.class));
            }
            if(object.has("EXECUTIONS")) {
                information.put("EXECUTIONS", JsonManager.checkDeserialize(context, object, "EXECUTIONS", int.class));
            }
            if(object.has("PREDICATE")) {
                information.put("PREDICATE", JsonManager.checkDeserialize(context, object, "PREDICATE", String.class));
            }
            if(object.has("STEPS_PER_EXECUTION")) {
                information.put("STEPS_PER_EXECUTION", JsonManager.checkDeserialize(context, object, "STEPS_PER_EXECUTION", int.class));
            }
            if(object.has("ENDING_PREDICATE")) {
                information.put("ENDING_PREDICATE", JsonManager.checkDeserialize(context, object, "ENDING_PREDICATE", String.class));
            }
            if(object.has("ENDING_TIME")) {
                information.put("ENDING_TIME", JsonManager.checkDeserialize(context, object, "ENDING_TIME", int.class));
            }
            if(object.has("START_AFTER_STEPS")) {
                information.put("START_AFTER_STEPS", JsonManager.checkDeserialize(context, object, "START_AFTER_STEPS", int.class));
            }
            if(object.has("STARTING_PREDICATE")) {
                information.put("STARTING_PREDICATE", JsonManager.checkDeserialize(context, object, "STARTING_PREDICATE", String.class));
            }
            if(object.has("STARTING_TIME")) {
                information.put("STARTING_TIME", JsonManager.checkDeserialize(context, object, "STARTING_TIME", int.class));
            }
            if(object.has("HYPOTHESIS_CHECKING_TYPE")) {
                information.put("HYPOTHESIS_CHECKING_TYPE", JsonManager.checkDeserialize(context, object, "HYPOTHESIS_CHECKING_TYPE", SimulationHypothesisChecker.HypothesisCheckingType.class));
            }
            if(object.has("ESTIMATION_TYPE")) {
                information.put("ESTIMATION_TYPE", JsonManager.checkDeserialize(context, object, "ESTIMATION_TYPE", SimulationEstimator.EstimationType.class));
            }
            if(object.has("CHECKING_TYPE")) {
                information.put("CHECKING_TYPE", JsonManager.checkDeserialize(context, object, "CHECKING_TYPE", SimulationCheckingType.class));
            }
            if(object.has("DESIRED_VALUE")) {
                information.put("DESIRED_VALUE", JsonManager.checkDeserialize(context, object, "DESIRED_VALUE", double.class));
            }
            if(object.has("SIGNIFICANCE")) {
                information.put("SIGNIFICANCE", JsonManager.checkDeserialize(context, object, "SIGNIFICANCE", double.class));
            }
            if(object.has("EPSILON")) {
                information.put("EPSILON", JsonManager.checkDeserialize(context, object, "EPSILON", double.class));
            }
            if(object.has("TIME")) {
                information.put("TIME", JsonManager.checkDeserialize(context, object, "TIME", int.class));
            }
        }

        public Map<String, Object> getInformation() {
            return information;
        }
    }

    public static final JsonDeserializer<SimulationItem> JSON_DESERIALIZER = SimulationItem::new;

    private SimulationType type;

    private Map<String, Object> information;

    private transient ObjectProperty<Checked> checked;

    private transient SimulationStats simulationStats;

    private transient ListProperty<Trace> traces;

    private transient ListProperty<List<Integer>> timestamps;

    public SimulationItem(SimulationType type, Map<String, Object> information) {
        this.type = type;
        this.information = information;
        initListeners();
    }

    private SimulationItem(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) {
        final JsonObject object = json.getAsJsonObject();
        this.type = JsonManager.checkDeserialize(context, object, "type", SimulationType.class);
        this.information = JsonManager.checkDeserialize(context, object, "information", SimulationCheckingInformation.class).getInformation();
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
        return type.getName();
    }

    public SimulationType getType() {
        return type;
    }
    @Override
    public int hashCode() {
        return Objects.hash(type, information);
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof SimulationItem)) {
            return false;
        }
        SimulationItem otherItem = (SimulationItem) obj;
        return this.type == otherItem.type && this.information.equals(otherItem.information);
    }

    public void reset() {
        // TODO
    }

    public Map<String, Object> getInformation() {
        return information;
    }

    public boolean containsField(String key) {
        return information.containsKey(key);
    }

    public Object getField(String key) {
        return information.get(key);
    }

    public String getConfiguration() {
        List<String> configurations = new ArrayList<>();
        for(String key : information.keySet()) {
            Object obj = information.get(key);
            if(obj instanceof ReplayTrace) {
                configurations.add(key + " : " + ((ReplayTrace) obj).getName());
            } else {
                configurations.add(key + " : " + obj.toString());
            }
        }
        if(!configurations.isEmpty()) {
            return String.join(",\n", configurations);
        }
        return type.name();
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
