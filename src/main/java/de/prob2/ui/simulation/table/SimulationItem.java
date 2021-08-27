package de.prob2.ui.simulation.table;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import de.prob.statespace.Trace;
import de.prob2.ui.simulation.choice.SimulationCheckingType;
import de.prob2.ui.simulation.choice.SimulationType;
import de.prob2.ui.simulation.simulators.check.SimulationEstimator;
import de.prob2.ui.simulation.simulators.check.SimulationHypothesisChecker;
import de.prob2.ui.simulation.simulators.check.SimulationStats;
import de.prob2.ui.verifications.Checked;

import de.prob2.ui.verifications.IExecutableItem;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;

public class SimulationItem implements IExecutableItem {

	public static class SimulationCheckingInformation {
		private final Map<String, Object> information;

		@JsonCreator
		private SimulationCheckingInformation() {
			information = new HashMap<>();
		}

		public Map<String, Object> getInformation() {
			return information;
		}

		@JsonProperty("PROBABILITY")
		private void setProbability(final double probability) {
			this.information.put("PROBABILITY", probability);
		}

		@JsonProperty("EXECUTIONS")
		private void setExecutions(final int executions) {
			this.information.put("EXECUTIONS", executions);
		}

		@JsonProperty("PREDICATE")
		private void setPredicate(final String predicate) {
			this.information.put("PREDICATE", predicate);
		}

		@JsonProperty("STEPS_PER_EXECUTION")
		private void setStepsPerExecution(final int stepsPerExecution) {
			this.information.put("STEPS_PER_EXECUTION", stepsPerExecution);
		}

		@JsonProperty("ENDING_PREDICATE")
		private void setEndingPredicate(final String endingPredicate) {
			this.information.put("ENDING_PREDICATE", endingPredicate);
		}

		@JsonProperty("ENDING_TIME")
		private void setEndingTime(final int endingTime) {
			this.information.put("ENDING_TIME", endingTime);
		}

		@JsonProperty("START_AFTER_STEPS")
		private void setStartAfterSteps(final int startAfterSteps) {
			this.information.put("START_AFTER_STEPS", startAfterSteps);
		}

		@JsonProperty("STARTING_PREDICATE")
		private void setStartingPredicate(final String startingPredicate) {
			this.information.put("STARTING_PREDICATE", startingPredicate);
		}

		@JsonProperty("STARTING_TIME")
		private void setStartingTime(final int startingTime) {
			this.information.put("STARTING_TIME", startingTime);
		}

		@JsonProperty("HYPOTHESIS_CHECKING_TYPE")
		private void setHypothesisCheckingType(final SimulationHypothesisChecker.HypothesisCheckingType hypothesisCheckingType) {
			this.information.put("HYPOTHESIS_CHECKING_TYPE", hypothesisCheckingType);
		}

		@JsonProperty("ESTIMATION_TYPE")
		private void setEstimationType(final SimulationEstimator.EstimationType estimationType) {
			this.information.put("ESTIMATION_TYPE", estimationType);
		}

		@JsonProperty("CHECKING_TYPE")
		private void setCheckingType(final SimulationCheckingType checkingType) {
			this.information.put("CHECKING_TYPE", checkingType);
		}

		@JsonProperty("DESIRED_VALUE")
		private void setDesiredValue(final double desiredValue) {
			this.information.put("DESIRED_VALUE", desiredValue);
		}

		@JsonProperty("SIGNIFICANCE")
		private void setSignificance(final double significance) {
			this.information.put("SIGNIFICANCE", significance);
		}

		@JsonProperty("EPSILON")
		private void setEpsilon(final double epsilon) {
			this.information.put("EPSILON", epsilon);
		}

		@JsonProperty("TIME")
		private void setTime(final int time) {
			this.information.put("TIME", time);
		}
	}

	private SimulationType type;

	private Map<String, Object> information;

	@JsonIgnore
	private ObjectProperty<Checked> checked;

	@JsonIgnore
	private SimulationStats simulationStats;

	@JsonIgnore
	private ListProperty<Trace> traces;

	@JsonIgnore
	private ListProperty<List<Integer>> timestamps;

	public SimulationItem(SimulationType type, Map<String, Object> information) {
		this.type = type;
		this.information = information;
		initListeners();
	}

	@JsonCreator
	private SimulationItem(
		@JsonProperty("type") final SimulationType type,
		@JsonProperty("information") final SimulationCheckingInformation information
	) {
		this(type, information.getInformation());
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

	@Override
	public boolean selected() {
		return true;
	}

	@Override
	public BooleanProperty selectedProperty() {
		return null;
	}

	@Override
	public void setSelected(boolean selected) {

	}

	public ObjectProperty<Checked> checkedProperty() {
		return checked;
	}

	public Checked getChecked() {
		return checked.get();
	}

	@JsonIgnore
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
		this.setChecked(Checked.NOT_CHECKED);
		this.simulationStats = null;
		this.timestamps.clear();
		this.traces.clear();
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

	@JsonIgnore
	public String getConfiguration() {
		List<String> configurations = new ArrayList<>();
		for(String key : information.keySet()) {
			Object obj = information.get(key);
			configurations.add(String.format("%s : %s", key, obj.toString()));
		}
		return String.join(",\n", configurations);
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

	public String createdByForMetadata() {
		String createdBy = "Simulation: " + getTypeAsName() + "; " + getConfiguration();
		return createdBy.replaceAll("\n", " ");
	}
}
