package de.prob2.ui.simulation.simulators.check;


import com.google.inject.Injector;
import de.prob.statespace.State;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.simulation.SimulationError;
import de.prob2.ui.simulation.SimulationHelperFunctions;
import de.prob2.ui.simulation.SimulatorStage;
import de.prob2.ui.simulation.simulators.Simulator;
import javafx.application.Platform;
import javafx.scene.control.Alert;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimulationMonteCarlo extends Simulator {

	public enum InitialType {
		NO_CONDITION("No Condition"),
		INITIAL_STEPS("Initial Number of Steps"),
		INITIAL_PREDICATE("Initial Predicate"),
		INITIAL_TIME("Initial Time");

		private final String name;

		InitialType(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}

	public enum StartingType {
		NO_CONDITION("No Condition"),
		START_AFTER_STEPS("Start after Number of Steps"),
		STARTING_PREDICATE("Starting Predicate"),
		STARTING_TIME("Starting Time");

		private final String name;

		StartingType(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}

	public enum EndingType {
		NUMBER_STEPS("Number Steps"),
		ENDING_PREDICATE("Ending Predicate"),
		ENDING_TIME("Ending Time");

		private final String name;

		EndingType(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}

	public enum MonteCarloCheckResult {
		NOT_FINISHED, SUCCESS, FAIL
	}

	private static final int MAX_NUMBER_STEPS_BEFORE_CHECKING = 1000;
	// This could also be a parameter of the hypothesis testing/estimation check.
	// Up to MAX_NUMBER_STEPS_BEFORE_CHECKING steps are simulated in the beginning before simulating until the initial condition, starting condition, and ending condition,
	// to check a property between the starting and ending condition.
	// This is done to vary the length of the simulation.
	// Otherwise, the property is always checked on traces where starting condition and ending condition are fulfilled once.


	protected final Injector injector;

	protected Map<String, List<Integer>> operationExecutions;

	protected Map<String, List<Integer>> operationEnablings;

	protected Map<String, List<Integer>> operationExecutionPercentage;

	protected List<List<Integer>> resultingTimestamps;

	protected List<Trace> resultingTraces;

	protected int numberExecutions;

	protected int currentNumberStepsBeforeChecking;

	protected boolean initialConditionReached;

	protected boolean startingConditionReached;

	protected int initialStepAt;

	protected int initialTimeAt;

	protected int startAtStep;

	protected int startAtTime;

	protected Map<String, Object> additionalInformation;

	protected SimulationStats stats;

	protected MonteCarloCheckResult result;

	public SimulationMonteCarlo(final Injector injector, final CurrentTrace currentTrace, int numberExecutions, Map<String, Object> additionalInformation) {
		super(currentTrace);
		this.injector = injector;
		this.operationExecutions = new HashMap<>();
		this.operationEnablings = new HashMap<>();
		this.operationExecutionPercentage = new HashMap<>();
		this.resultingTraces = new ArrayList<>();
		this.resultingTimestamps = new ArrayList<>();
		this.numberExecutions = numberExecutions;
		this.initialConditionReached = false;
		this.startingConditionReached = false;
		this.currentNumberStepsBeforeChecking = Integer.MAX_VALUE;
		this.initialStepAt = Integer.MAX_VALUE;
		this.initialTimeAt = Integer.MAX_VALUE;
		this.startAtStep = Integer.MAX_VALUE;
		this.startAtTime = Integer.MAX_VALUE;
		this.additionalInformation = additionalInformation;
		this.stats = null;
		this.result = MonteCarloCheckResult.NOT_FINISHED;
	}

	@Override
	public boolean endingConditionReached(Trace trace) {
		if(super.endingConditionReached(trace)) {
			return true;
		}
		if(!initialConditionReached || !startingConditionReached) {
			return false;
		}
		if(additionalInformation.containsKey("STEPS_PER_EXECUTION")) {
			int stepsPerExecution = (int) additionalInformation.get("STEPS_PER_EXECUTION");
			return stepCounter >= stepsPerExecution + startAtStep;
		} else if(additionalInformation.containsKey("ENDING_PREDICATE")) {
			String predicate = (String) additionalInformation.get("ENDING_PREDICATE");
			State state = trace.getCurrentState();
			SimulationHelperFunctions.EvaluationMode mode = SimulationHelperFunctions.extractMode(currentTrace.getModel());
			String evalResult = simulationEventHandler.getCache().readValueWithCaching(state, predicate, mode);
			if("TRUE".equals(evalResult)) {
				return true;
			} else if(!"FALSE".equals(evalResult) && !evalResult.startsWith("NOT-INITIALISED")) {
				throw new SimulationError("Ending predicate is not of type boolean");
			}
		} else if(additionalInformation.containsKey("ENDING_TIME")) {
			int endingTime = (int) additionalInformation.get("ENDING_TIME");
			return time.get() > startAtTime + endingTime;
		}
		return false;
	}

	@Override
	public void updateInitialInformation(Trace trace) {
		super.updateInitialInformation(trace);
		if(stepCounter < currentNumberStepsBeforeChecking || initialConditionReached) {
			return;
		}
		if(additionalInformation.containsKey("INITIAL_STEPS")) {
			int initialSteps = (int) additionalInformation.get("INITIAL_STEPS");
			if(stepCounter >= initialSteps) {
				setInitialInformation();
			}
		} else if(additionalInformation.containsKey("INITIAL_PREDICATE")) {
			String predicate = (String) additionalInformation.get("INITIAL_PREDICATE");
			State state = trace.getCurrentState();
			SimulationHelperFunctions.EvaluationMode mode = SimulationHelperFunctions.extractMode(currentTrace.getModel());
			String evalResult = simulationEventHandler.getCache().readValueWithCaching(state, predicate, mode);
			if("TRUE".equals(evalResult)) {
				setInitialInformation();
			} else if(!"FALSE".equals(evalResult) && !evalResult.startsWith("NOT-INITIALISED")) {
				throw new SimulationError("Initial predicate is not of type boolean");
			}
		} else if(additionalInformation.containsKey("INITIAL_TIME")) {
			int initialTime = (int) additionalInformation.get("INITIAL_TIME");
			if(time.get() >= initialTime) {
				setInitialInformation();
			}
		} else {
			setInitialInformation();
		}
	}

	@Override
	public void updateStartingInformation(Trace trace) {
		super.updateStartingInformation(trace);
		if(!initialConditionReached || startingConditionReached) {
			return;
		}
		if(additionalInformation.containsKey("START_AFTER_STEPS")) {
			int startAfterSteps = (int) additionalInformation.get("START_AFTER_STEPS");
			if(stepCounter >= startAfterSteps + initialStepAt) {
				setStartingInformation();
			}
		} else if(additionalInformation.containsKey("STARTING_PREDICATE")) {
			String predicate = (String) additionalInformation.get("STARTING_PREDICATE");
			State state = trace.getCurrentState();
			SimulationHelperFunctions.EvaluationMode mode = SimulationHelperFunctions.extractMode(currentTrace.getModel());
			String evalResult = simulationEventHandler.getCache().readValueWithCaching(state, predicate, mode);
			if("TRUE".equals(evalResult)) {
				setStartingInformation();
			} else if(!"FALSE".equals(evalResult) && !evalResult.startsWith("NOT-INITIALISED")) {
				throw new SimulationError("Starting predicate is not of type boolean");
			}
		} else if(additionalInformation.containsKey("STARTING_TIME")) {
			int startingTime = (int) additionalInformation.get("STARTING_TIME");
			if(time.get() >= startingTime + initialTimeAt) {
				setStartingInformation();
			}
		} else {
			setStartingInformation();
		}
	}

	private void setInitialInformation() {
		initialConditionReached = true;
		initialStepAt = stepCounter;
		initialTimeAt = time.get();
	}

	private void setStartingInformation() {
		startingConditionReached = true;
		startAtStep = stepCounter;
		startAtTime = time.get();
	}

	@Override
	public void run() {
		Trace startTrace = new Trace(currentTrace.get().getStateSpace());

		long wallTime = 0;
		try {
			startTrace.getStateSpace().startTransaction();
			wallTime = System.currentTimeMillis();
			for (int i = 0; i < numberExecutions; i++) {
				currentNumberStepsBeforeChecking = (int) (Math.random() * MAX_NUMBER_STEPS_BEFORE_CHECKING);
				Trace newTrace = startTrace;
				setupBeforeSimulation(newTrace);
				while (!endingConditionReached(newTrace)) {
					newTrace = simulationStep(newTrace);
				}
				resultingTraces.add(newTrace);
				resultingTimestamps.add(getTimestamps());
				checkTrace(newTrace, time.get());
				collectOperationStatistics(newTrace);
				resetSimulator();
			}
			check();
		} catch (SimulationError e) {
			this.result = MonteCarloCheckResult.FAIL;
			Platform.runLater(() -> {
				final Alert alert = injector.getInstance(StageManager.class).makeExceptionAlert(e, "simulation.error.header.runtime", "simulation.error.body.runtime");
				alert.initOwner(injector.getInstance(SimulatorStage.class));
				alert.showAndWait();
			});
		} finally {
			wallTime = System.currentTimeMillis() - wallTime;
			startTrace.getStateSpace().endTransaction();
		}
		calculateStatistics(wallTime);
	}

	public void check() {
		if(this.resultingTraces.size() == numberExecutions) {
			this.result = MonteCarloCheckResult.SUCCESS;
		} else {
			this.result = MonteCarloCheckResult.FAIL;
		}
	}

	public void checkTrace(Trace trace, int time) {
		// Monte Carlo Simulation does not apply any checks on a trace. But classes inheriting from SimulationMonteCarlo might apply some checks
	}

	private void collectOperationStatistics(Trace trace) {
		Map<String, Integer> operationExecutionsTrace = new HashMap<>();
		Map<String, Integer> operationEnablingsTrace = new HashMap<>();
		for(Transition transition : trace.getTransitionList()) {
			String opName = transition.getName();

			//update executed operations
			operationExecutionsTrace.putIfAbsent(opName, 0);
			operationExecutionsTrace.computeIfPresent(opName, (key, val) -> val + 1);

			// update enabled operations
			simulationEventHandler.getCache().readEnabledOperationsWithCaching(transition.getSource())
					.forEach(enabledOp -> {
						operationEnablingsTrace.putIfAbsent(enabledOp, 0);
						operationEnablingsTrace.computeIfPresent(enabledOp, (key, val) -> val + 1);
					});
		}
		for(String key : operationEnablingsTrace.keySet()) {
			//update enabled operations for all traces
			int addedEnabling = operationEnablingsTrace.getOrDefault(key, 0);

			operationEnablings.putIfAbsent(key, new ArrayList<>());
			operationEnablings.get(key).add(addedEnabling);

			//update executed operations for all traces
			int addedExecution = operationExecutionsTrace.getOrDefault(key, 0);
			operationExecutions.putIfAbsent(key, new ArrayList<>());
			operationExecutions.get(key).add(addedExecution);
		}
	}

	@Override
	public void resetSimulator() {
		super.resetSimulator();
		this.initialConditionReached = false;
		this.startingConditionReached = false;
		this.startAtStep = Integer.MAX_VALUE;
		this.startAtTime = Integer.MAX_VALUE;
	}

	protected void calculateStatistics(long time) {
		double wallTime = new BigDecimal(time / 1000.0f).setScale(3, RoundingMode.HALF_UP).doubleValue();
		stats = new SimulationStats(this.numberExecutions, this.numberExecutions, 1.0, wallTime, calculateExtendedStats());
	}

	public SimulationExtendedStats calculateExtendedStats() {
		Map<String, Integer> executionsResult = new HashMap<>();
		Map<String, Integer> enablingsResult = new HashMap<>();
		Map<String, Double> percentageResult = new HashMap<>();
		for(String key : operationEnablings.keySet()) {
			int absoluteExecutions = operationExecutions.get(key).stream().reduce(0, Integer::sum);
			int absoluteEnablings = operationEnablings.get(key).stream().reduce(0, Integer::sum);
			int operationExecutionsValue = (int) Math.round((double) absoluteExecutions);
			int operationEnablingsValue = (int) Math.round((double) absoluteEnablings);
			executionsResult.put(key, operationExecutionsValue);
			enablingsResult.put(key, operationEnablingsValue);
			percentageResult.put(key, (double) absoluteExecutions/absoluteEnablings);
		}
		return new SimulationExtendedStats(executionsResult, enablingsResult, percentageResult);
	}

	public List<Trace> getResultingTraces() {
		return resultingTraces;
	}

	public List<List<Integer>> getResultingTimestamps() {
		return resultingTimestamps;
	}

	public SimulationStats getStats() {
		return stats;
	}

	public MonteCarloCheckResult getResult() {
		return result;
	}
}
