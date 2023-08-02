package de.prob2.ui.simulation.simulators.check;


import com.google.inject.Injector;
import de.prob.statespace.State;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.simulation.SimulationError;
import de.prob2.ui.simulation.SimulationHelperFunctions;
import de.prob2.ui.simulation.SimulatorStage;
import de.prob2.ui.simulation.configuration.SimulationBlackBoxModelConfiguration;
import de.prob2.ui.simulation.configuration.SimulationFileHandler;
import de.prob2.ui.simulation.simulators.Simulator;
import de.prob2.ui.verifications.Checked;
import javafx.application.Platform;
import javafx.scene.control.Alert;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SimulationCheckingSimulator extends Simulator implements ISimulationPropertyChecker {

	public enum StartingType {
		START_AFTER_STEPS("simulation.type.start.afterSteps"),
		STARTING_PREDICATE("simulation.type.start.predicate"),
		STARTING_PREDICATE_ACTIVATED("simulation.type.start.predicateActivated"),
		STARTING_TIME("simulation.type.start.time");

		private final String key;

		StartingType(String key) {
			this.key = key;
		}

		public String getKey() {
			return key;
		}

		public String getName(I18n i18n) {
			return i18n.translate(key);
		}
	}

	public enum EndingType {
		NUMBER_STEPS("simulation.type.end.steps"),
		ENDING_PREDICATE("simulation.type.end.predicate"),
		ENDING_TIME("simulation.type.end.time");

		private final String key;

		EndingType(String key) {
			this.key = key;
		}

		public String getKey() {
			return key;
		}

		public String getName(I18n i18n) {
			return i18n.translate(key);
		}
	}

	public enum MonteCarloCheckResult {
		NOT_FINISHED, SUCCESS, FAIL
	}

	private final Injector injector;

	private final Map<String, List<Integer>> operationExecutions;

	private final Map<String, List<Integer>> operationEnablings;

	private final List<List<Integer>> resultingTimestamps;

	private final List<Trace> resultingTraces;

	private final List<Checked> resultingStatus;

	private final int numberExecutions;

	private final int maxStepsBeforeProperty;

	private List<Path> blackBoxTimedTraces;

	private int currentNumberStepsBeforeChecking;

	private boolean startingConditionReached;

	private int startAtStep;

	private int startAtTime;

	private final Map<String, Object> additionalInformation;

	private SimulationStats stats;

	private MonteCarloCheckResult result;

	public SimulationCheckingSimulator(final Injector injector, final CurrentTrace currentTrace, int numberExecutions, int maxStepsBeforeProperty, Map<String, Object> additionalInformation) {
		super(currentTrace);
		this.injector = injector;
		this.operationExecutions = new HashMap<>();
		this.operationEnablings = new HashMap<>();
		this.resultingTraces = new ArrayList<>();
		this.resultingTimestamps = new ArrayList<>();
		this.resultingStatus = new ArrayList<>();
		this.numberExecutions = numberExecutions;
		this.maxStepsBeforeProperty = maxStepsBeforeProperty;
		this.blackBoxTimedTraces = new ArrayList<>();
		this.startingConditionReached = false;
		this.currentNumberStepsBeforeChecking = Integer.MAX_VALUE;
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
		if(!startingConditionReached) {
			return false;
		}
		if(additionalInformation.containsKey("STEPS_PER_EXECUTION")) {
			int stepsPerExecution = (int) additionalInformation.get("STEPS_PER_EXECUTION");
			return stepCounter >= stepsPerExecution + startAtStep;
		} else if(additionalInformation.containsKey("ENDING_PREDICATE")) {
			String predicate = (String) additionalInformation.get("ENDING_PREDICATE");
			State state = trace.getCurrentState();
			SimulationHelperFunctions.EvaluationMode mode = SimulationHelperFunctions.extractMode(currentTrace.getModel());
			String evalResult = simulationEventHandler.getCache().readValueWithCaching(state, this.getVariables(), predicate, mode);
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
	public void updateStartingInformation(Trace trace) {
		super.updateStartingInformation(trace);
		if(stepCounter < currentNumberStepsBeforeChecking || startingConditionReached) {
			return;
		}
		if(additionalInformation.containsKey("START_AFTER_STEPS")) {
			int startAfterSteps = (int) additionalInformation.get("START_AFTER_STEPS");
			if(stepCounter >= startAfterSteps) {
				setStartingInformation();
			}
		} else if(additionalInformation.containsKey("STARTING_PREDICATE")) {
			String predicate = (String) additionalInformation.get("STARTING_PREDICATE");
			State state = trace.getCurrentState();
			SimulationHelperFunctions.EvaluationMode mode = SimulationHelperFunctions.extractMode(currentTrace.getModel());
			String evalResult = simulationEventHandler.getCache().readValueWithCaching(state, this.getVariables(), predicate, mode);
			if ("TRUE".equals(evalResult)) {
				setStartingInformation();
			} else if (!"FALSE".equals(evalResult) && !evalResult.startsWith("NOT-INITIALISED")) {
				throw new SimulationError("Starting predicate is not of type boolean");
			}
		} else if(additionalInformation.containsKey("STARTING_PREDICATE_ACTIVATED")) {

			State previousState = trace.getPreviousState();

			if(previousState == null || !previousState.isInitialised()) {
				return;
			}

			String predicate = (String) additionalInformation.get("STARTING_PREDICATE_ACTIVATED");
			SimulationHelperFunctions.EvaluationMode previousMode = SimulationHelperFunctions.extractMode(currentTrace.getModel());
			String previousEvalResult = simulationEventHandler.getCache().readValueWithCaching(previousState, this.getVariables(), predicate, previousMode);

			State currentState = trace.getCurrentState();
			SimulationHelperFunctions.EvaluationMode currentMode = SimulationHelperFunctions.extractMode(currentTrace.getModel());
			String currentEvalResult = simulationEventHandler.getCache().readValueWithCaching(currentState, this.getVariables(), predicate, currentMode);


			if ("TRUE".equals(currentEvalResult)) {
				if("FALSE".equals(previousEvalResult)) {
					setStartingInformation();
				}
			} else if (!"FALSE".equals(currentEvalResult) && !currentEvalResult.startsWith("NOT-INITIALISED")) {
				throw new SimulationError("Starting predicate is not of type boolean");
			}
		} else if(additionalInformation.containsKey("STARTING_TIME")) {
			int startingTime = (int) additionalInformation.get("STARTING_TIME");
			if(time.get() >= startingTime) {
				setStartingInformation();
			}
		} else {
			setStartingInformation();
		}
	}

	private void setStartingInformation() {
		startingConditionReached = true;
		startAtStep = stepCounter;
		startAtTime = time.get();
	}

	@Override
	public void run() {
		run(this);
	}

	private void initBlackBoxBeforeSimulation(boolean isBlackBox) {
		blackBoxTimedTraces.clear();
		if(isBlackBox) {
			blackBoxTimedTraces = ((SimulationBlackBoxModelConfiguration) config).getTimedTraces();
		}
	}

	private void initForBlackBoxValidationIfNecessary(boolean isBlackBox, int index) {
		if(isBlackBox) {
			try {
				this.initSimulator(SimulationFileHandler.constructConfiguration(blackBoxTimedTraces.get(index)));
			} catch (Exception e) {
				final Alert alert = injector.getInstance(StageManager.class).makeExceptionAlert(e, "simulation.error.header.fileNotFound", "simulation.error.body.fileNotFound");
				alert.initOwner(injector.getInstance(SimulatorStage.class));
				alert.showAndWait();
			}
		}
	}

	@Override
	public void run(ISimulationPropertyChecker simulationPropertyChecker) {
		boolean isBlackBox = config instanceof SimulationBlackBoxModelConfiguration;
		Trace startTrace = new Trace(currentTrace.get().getStateSpace());
		initBlackBoxBeforeSimulation(isBlackBox);

		long wallTime = 0;
		try {
			startTrace.getStateSpace().startTransaction();
			wallTime = System.currentTimeMillis();
			for (int i = 0; i < numberExecutions; i++) {
				initForBlackBoxValidationIfNecessary(isBlackBox, i);
				currentNumberStepsBeforeChecking = (int) (Math.random() * maxStepsBeforeProperty);
				Trace newTrace = startTrace;
				setupBeforeSimulation(newTrace);
				while (!endingConditionReached(newTrace)) {
					newTrace = simulationStep(newTrace);
				}
				resultingTraces.add(newTrace);
				resultingTimestamps.add(getTimestamps());
				Checked checked = simulationPropertyChecker.checkTrace(newTrace, time.get());
				resultingStatus.add(checked);
				collectOperationStatistics(newTrace);
				resetSimulator();
			}
			simulationPropertyChecker.check();
		} catch (SimulationError e) {
			simulationPropertyChecker.setResult(MonteCarloCheckResult.FAIL);
			Platform.runLater(() -> {
				final Alert alert = injector.getInstance(StageManager.class).makeExceptionAlert(e, "simulation.error.header.runtime", "simulation.error.body.runtime");
				alert.initOwner(injector.getInstance(SimulatorStage.class));
				alert.showAndWait();
			});
		} finally {
			wallTime = System.currentTimeMillis() - wallTime;
			startTrace.getStateSpace().endTransaction();
		}
		simulationPropertyChecker.calculateStatistics(wallTime);
	}

	public void check() {
		if(this.resultingTraces.size() == numberExecutions) {
			this.result = MonteCarloCheckResult.SUCCESS;
		} else {
			this.result = MonteCarloCheckResult.FAIL;
		}
	}

	@Override
	public Checked checkTrace(Trace trace, int time) {
		// Monte Carlo Simulation does not apply any checks on a trace. But classes inheriting from SimulationMonteCarlo might apply some checks
		return Checked.SUCCESS;
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
			transition.getSource().getOutTransitions().stream()
					.map(Transition::getName)
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
		this.startingConditionReached = false;
		this.startAtStep = Integer.MAX_VALUE;
		this.startAtTime = Integer.MAX_VALUE;
	}

	@Override
	public void calculateStatistics(long time) {
		double wallTime = new BigDecimal(time / 1000.0f).setScale(3, RoundingMode.HALF_UP).doubleValue();
		double averageTraceLength = operationExecutions.values().stream()
				.map(l -> l.stream().reduce(0, Integer::sum))
				.reduce(0, Integer::sum)/(double) numberExecutions;
		this.stats = new SimulationStats(this.numberExecutions, this.numberExecutions, 1.0, 0.0, wallTime, averageTraceLength, calculateExtendedStats());
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

	public Map<String, List<Integer>> getOperationExecutions() {
		return operationExecutions;
	}

	public List<Trace> getResultingTraces() {
		return resultingTraces;
	}

	public List<List<Integer>> getResultingTimestamps() {
		return resultingTimestamps;
	}

	public List<Checked> getResultingStatus() {
		return resultingStatus;
	}

	public SimulationStats getStats() {
		return stats;
	}

	@Override
	public void setStats(SimulationStats stats) {
		this.stats = stats;
	}

	public MonteCarloCheckResult getResult() {
		return result;
	}

	@Override
	public void setResult(SimulationCheckingSimulator.MonteCarloCheckResult result) {
		this.result = result;
	}

	@Override
	public int getNumberSuccess() {
		return resultingTraces.size();
	}

	public Map<String, Object> getAdditionalInformation() {
		return additionalInformation;
	}

	public int getStartAtStep() {
		return startAtStep;
	}

	public int getStartAtTime() {
		return startAtTime;
	}
}
