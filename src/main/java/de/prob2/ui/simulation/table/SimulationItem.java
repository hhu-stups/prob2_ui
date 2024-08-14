package de.prob2.ui.simulation.table;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.common.base.MoreObjects;

import de.prob.statespace.Trace;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.simulation.choice.SimulationCheckingType;
import de.prob2.ui.simulation.choice.SimulationType;
import de.prob2.ui.simulation.simulators.check.SimulationEstimator;
import de.prob2.ui.simulation.simulators.check.SimulationHypothesisChecker;
import de.prob2.ui.simulation.simulators.check.SimulationStats;
import de.prob2.ui.verifications.CheckingExecutors;
import de.prob2.ui.verifications.CheckingStatus;
import de.prob2.ui.verifications.ExecutionContext;
import de.prob2.ui.verifications.ITraceTask;
import de.prob2.ui.verifications.type.BuiltinValidationTaskTypes;
import de.prob2.ui.verifications.type.ValidationTaskType;

import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;

@JsonPropertyOrder({
	"id",
	"simulationPath",
	"type",
	"information",
})
public final class SimulationItem implements ITraceTask {
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private String id;
	private final Path simulationPath;
	private SimulationType type;
	private Map<String, Object> information;
	@JsonIgnore
	private ObjectProperty<CheckingStatus> status;
	@JsonIgnore
	private SimulationStats simulationStats;
	@JsonIgnore
	private ListProperty<Trace> traces;
	@JsonIgnore
	private ListProperty<List<Integer>> timestamps;
	@JsonIgnore
	private ListProperty<CheckingStatus> statuses;

	public SimulationItem(String id, Path simulationPath, SimulationType type, Map<String, Object> information) {
		this.id = id;
		this.simulationPath = Objects.requireNonNull(simulationPath, "simulationPath");
		this.type = Objects.requireNonNull(type, "type");
		this.information = Objects.requireNonNull(information, "information");
		initListeners();
	}

	@JsonCreator
	private SimulationItem(@JsonProperty("id") String id, @JsonProperty("simulationPath") Path simulationPath, @JsonProperty("type") SimulationType type, @JsonProperty("information") SimulationCheckingInformation information) {
		this(id, simulationPath, type, information.getInformation());
	}

	private void initListeners() {
		this.status = new SimpleObjectProperty<>(this, "status", CheckingStatus.NOT_CHECKED);
		this.traces = new SimpleListProperty<>(FXCollections.observableArrayList());
		this.timestamps = new SimpleListProperty<>(FXCollections.observableArrayList());
		this.statuses = new SimpleListProperty<>(FXCollections.observableArrayList());
		this.simulationStats = null;
	}

	@Override
	public String getId() {
		return this.id;
	}

	@Override
	public ValidationTaskType<SimulationItem> getTaskType() {
		return BuiltinValidationTaskTypes.SIMULATION;
	}

	@Override
	public String getTaskType(final I18n i18n) {
		return i18n.translate(this.getType());
	}

	@Override
	public ObjectProperty<CheckingStatus> statusProperty() {
		return status;
	}

	@Override
	public CheckingStatus getStatus() {
		return status.get();
	}

	@JsonIgnore
	public void setStatus(CheckingStatus status) {
		this.status.set(status);
	}

	@JsonIgnore
	public String getTypeAsName() {
		return type.toString();
	}

	public Path getSimulationPath() {
		return this.simulationPath;
	}

	public SimulationType getType() {
		return type;
	}

	public Map<String, Object> getInformation() {
		return information;
	}

	public boolean containsField(String key) {
		return information.containsKey(key);
	}

	@JsonIgnore
	public Object getField(String key) {
		return information.get(key);
	}

	@JsonIgnore
	public String getConfiguration() {
		return information.entrySet().stream()
			       .map(entry -> String.format(Locale.ROOT, "%s : %s", entry.getKey(), entry.getValue()))
			       .collect(Collectors.joining(",\n"));
	}

	@Override
	public String getTaskDescription(final I18n i18n) {
		return this.getConfiguration();
	}

	public ListProperty<Trace> tracesProperty() {
		return traces;
	}

	@JsonIgnore
	public List<Trace> getTraces() {
		return traces.get();
	}

	@JsonIgnore
	public void setTraces(List<Trace> traces) {
		this.traces.setAll(traces);
	}

	@Override
	public Trace getTrace() {
		return this.getTraces().isEmpty() ? null : this.getTraces().get(0);
	}

	public ListProperty<List<Integer>> timestampsProperty() {
		return timestamps;
	}

	@JsonIgnore
	public List<List<Integer>> getTimestamps() {
		return timestamps.get();
	}

	@JsonIgnore
	public void setTimestamps(List<List<Integer>> timestamps) {
		this.timestamps.setAll(timestamps);
	}

	public ListProperty<CheckingStatus> statusesProperty() {
		return statuses;
	}

	@JsonIgnore
	public List<CheckingStatus> getStatuses() {
		return statuses.get();
	}

	@JsonIgnore
	public void setStatuses(List<CheckingStatus> statuses) {
		this.statuses.setAll(statuses);
	}

	@JsonIgnore
	public SimulationStats getSimulationStats() {
		return simulationStats;
	}

	@JsonIgnore
	public void setSimulationStats(SimulationStats simulationStats) {
		this.simulationStats = simulationStats;
	}

	@JsonIgnore
	public void setId(String id) {
		this.id = id;
	}

	@JsonIgnore
	public void setSimulationType(SimulationType type) {
		this.type = type;
	}

	@JsonIgnore
	public void setInformation(Map<String, Object> information) {
		this.information = information;
	}

	public String createdByForMetadata() {
		String createdBy = "Simulation: " + getTypeAsName() + "; " + getConfiguration();
		return createdBy.replaceAll("\n", " ");
	}

	@Override
	public CompletableFuture<?> execute(CheckingExecutors executors, ExecutionContext context) {
		executors.simulationItemHandler().checkItem(this);
		// TODO Make SimulationItemHandler.checkItem return a correct CompletableFuture!
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public void resetAnimatorDependentState() {
		// The timestamps aren't directly dependent on the animator,
		// but they only make sense in combination with the traces,
		// so clear them both.
		this.timestamps.clear();
		this.traces.clear();
	}

	@Override
	public void reset() {
		this.setStatus(CheckingStatus.NOT_CHECKED);
		this.simulationStats = null;
		this.resetAnimatorDependentState();
	}

	@Override
	public boolean settingsEqual(Object other) {
		return other instanceof SimulationItem that
			       && Objects.equals(this.getTaskType(), that.getTaskType())
			       && Objects.equals(this.getId(), that.getId())
			       && Objects.equals(this.getSimulationPath(), that.getSimulationPath())
			       && Objects.equals(this.getType(), that.getType())
			       && Objects.equals(this.getInformation(), that.getInformation());
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
			       .add("id", this.getId())
			       .add("simulationPath", this.getSimulationPath())
			       .add("type", this.getType())
			       .add("information", this.getInformation())
			       .toString();
	}

	public SimulationItem withSimulationPath(int executions, Path simulationPath) {
		Map<String, Object> information = new HashMap<>(this.getInformation());
		// This could be the case if a SimB validation task is copied from a directory
		// of timed traces to a SimB simulation or to an external simulation
		if(!information.containsKey("EXECUTIONS")) {
			information.put("EXECUTIONS", executions);
		}
		return new SimulationItem(this.getId(), simulationPath, this.getType(), information);
	}

	public static final class SimulationCheckingInformation {

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

		@JsonProperty("MAX_STEPS_BEFORE_PROPERTY")
		private void setMaxStepsBeforeProperty(final int maxStepsBeforeProperty) {
			this.information.put("MAX_STEPS_BEFORE_PROPERTY", maxStepsBeforeProperty);
		}

		@JsonProperty("PREDICATE")
		private void setPredicate(final String predicate) {
			this.information.put("PREDICATE", predicate);
		}

		@JsonProperty("EXPRESSION")
		private void setExpression(final String expression) {
			this.information.put("EXPRESSION", expression);
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

		@JsonProperty("STARTING_PREDICATE_ACTIVATED")
		private void setStartingPredicateActivated(final String startingPredicateActivated) {
			this.information.put("STARTING_PREDICATE_ACTIVATED", startingPredicateActivated);
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
}
