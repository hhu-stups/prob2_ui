package de.prob2.ui.simulation;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.common.base.MoreObjects;

import de.prob.statespace.Trace;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.simulation.choice.SimulationCheckingType;
import de.prob2.ui.simulation.choice.SimulationType;
import de.prob2.ui.simulation.simulators.check.SimulationCheckingSimulator;
import de.prob2.ui.simulation.simulators.check.SimulationEstimator;
import de.prob2.ui.simulation.simulators.check.SimulationHypothesisChecker;
import de.prob2.ui.simulation.simulators.check.SimulationStats;
import de.prob2.ui.verifications.AbstractCheckableItem;
import de.prob2.ui.verifications.CheckingExecutors;
import de.prob2.ui.verifications.CheckingStatus;
import de.prob2.ui.verifications.ExecutionContext;
import de.prob2.ui.verifications.ICheckingResult;
import de.prob2.ui.verifications.type.BuiltinValidationTaskTypes;
import de.prob2.ui.verifications.type.ValidationTaskType;

@JsonPropertyOrder({
	"id",
	"simulationPath",
	"type",
	"information",
})
public final class SimulationItem extends AbstractCheckableItem {
	public static final class Result implements ICheckingResult {
		private final SimulationCheckingSimulator.MonteCarloCheckResult result;
		private final List<Trace> traces;
		private final List<List<Integer>> timestamps;
		private final List<CheckingStatus> statuses;
		private final SimulationStats stats;

		public Result(
			SimulationCheckingSimulator.MonteCarloCheckResult result,
			List<Trace> traces,
			List<List<Integer>> timestamps,
			List<CheckingStatus> statuses,
			SimulationStats stats
		) {
			this.result = Objects.requireNonNull(result, "result");
			this.traces = Objects.requireNonNull(traces, "traces");
			this.timestamps = Objects.requireNonNull(timestamps, "timestamps");
			this.statuses = Objects.requireNonNull(statuses, "statuses");
			this.stats = Objects.requireNonNull(stats, "stats");
		}

		public SimulationCheckingSimulator.MonteCarloCheckResult getResult() {
			return this.result;
		}

		@Override
		public CheckingStatus getStatus() {
			return switch (this.getResult()) {
				case SUCCESS -> CheckingStatus.SUCCESS;
				case FAIL -> CheckingStatus.FAIL;
				case NOT_FINISHED -> CheckingStatus.NOT_CHECKED;
				default -> throw new AssertionError("Unhandled simulation checker result: " + this.getResult());
			};
		}

		@Override
		public List<Trace> getTraces() {
			return Collections.unmodifiableList(this.traces);
		}

		public List<List<Integer>> getTimestamps() {
			return Collections.unmodifiableList(this.timestamps);
		}

		public List<CheckingStatus> getStatuses() {
			return Collections.unmodifiableList(this.statuses);
		}

		public SimulationStats getStats() {
			return this.stats;
		}

		// TODO We could implement withoutAnimatorDependentState using a custom result type so that the stats are kept
		// (everything else is animator-dependent and must be discarded)
	}

	private final Path simulationPath;
	private final SimulationType type;
	private final Map<String, Object> information;

	public SimulationItem(String id, Path simulationPath, SimulationType type, Map<String, Object> information) {
		super(id);

		this.simulationPath = Objects.requireNonNull(simulationPath, "simulationPath");
		this.type = Objects.requireNonNull(type, "type");
		this.information = Objects.requireNonNull(information, "information");
	}

	@JsonCreator
	private SimulationItem(@JsonProperty("id") String id, @JsonProperty("simulationPath") Path simulationPath, @JsonProperty("type") SimulationType type, @JsonProperty("information") SimulationCheckingInformation information) {
		this(id, simulationPath, type, information.getInformation());
	}

	@Override
	public ValidationTaskType<SimulationItem> getTaskType() {
		return BuiltinValidationTaskTypes.SIMULATION;
	}

	@Override
	public String getTaskType(final I18n i18n) {
		return i18n.translate(this.getType());
	}

	// The selected property from AbstractCheckableItem is (currently?) not used for SimulationItem,
	// so exclude it from JSON de-/serialization.

	@JsonIgnore
	@Override
	public boolean selected() {
		return super.selected();
	}

	@JsonIgnore
	@Override
	public void setSelected(boolean selected) {
		super.setSelected(selected);
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
		return Collections.unmodifiableMap(information);
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
	public boolean settingsEqual(Object other) {
		return super.settingsEqual(other)
			&& other instanceof SimulationItem that
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
