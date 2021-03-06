package de.prob2.ui.simulation.simulators.check;


import de.prob.statespace.State;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.simulation.simulators.Simulator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SimulationMonteCarlo extends Simulator {

	public enum EndingType {
		NUMBER_STEPS("Number Steps"),
		ENDING_PREDICATE("Ending Predicate"),
		ENDING_TIME("Ending Time");

		private String name;

		EndingType(String name) {
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

		private String name;

		StartingType(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}

	protected Map<String, List<Integer>> operationExecutions;

	protected Map<String, List<Integer>> operationEnablings;

	protected Map<String, List<Integer>> operationExecutionPercentage;

	protected List<List<Integer>> resultingTimestamps;

    protected List<Trace> resultingTraces;

    protected Trace trace;

    protected int numberExecutions;

	protected boolean startingConditionReached;

	protected int startAtStep;

	protected int startAtTime;

    protected Map<String, Object> additionalInformation;

	protected SimulationStats stats;

    public SimulationMonteCarlo(final CurrentTrace currentTrace, Trace trace, int numberExecutions, Map<String, Object> additionalInformation) {
        super(currentTrace);
        this.operationExecutions = new HashMap<>();
        this.operationEnablings = new HashMap<>();
        this.operationExecutionPercentage = new HashMap<>();
        this.resultingTraces = new ArrayList<>();
        this.resultingTimestamps = new ArrayList<>();
        this.trace = trace;
        this.numberExecutions = numberExecutions;
        this.startingConditionReached = false;
        this.startAtStep = Integer.MAX_VALUE;
        this.startAtTime = Integer.MAX_VALUE;
        this.additionalInformation = additionalInformation;
        this.stats = null;
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
			String evalResult = cache.readValueWithCaching(state, predicate);
			return "TRUE".equals(evalResult);
		} else if(additionalInformation.containsKey("ENDING_TIME")) {
			int endingTime = (int) additionalInformation.get("ENDING_TIME");
			return time.get() > startAtTime + endingTime;
		}
		return false;
	}

	@Override
	public void updateStartingInformation(Trace trace) {
    	if(startingConditionReached) {
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
			String evalResult = cache.readValueWithCaching(state, predicate);
			if("TRUE".equals(evalResult)) {
				setStartingInformation();
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
		Trace startTrace = new Trace(trace.getStateSpace());

		try {
			startTrace.getStateSpace().startTransaction();

			for (int i = 0; i < numberExecutions; i++) {
				Trace newTrace = startTrace;
				setupBeforeSimulation(newTrace);
				while (!endingConditionReached(newTrace)) {
					newTrace = simulationStep(newTrace);
				}
				resultingTraces.add(newTrace);
				resultingTimestamps.add(getTimestamps());
				checkTrace(newTrace, time.get());
				// TODO: Checkbox for extended statistics
				collectOperationStatistics(newTrace);
				resetSimulator();
			}
			check();
		} finally {
			startTrace.getStateSpace().endTransaction();
		}
		calculateStatistics();
    }

    public void check() {
		// Monte Carlo Simulation does not apply any checks. But classes inheriting from SimulationMonteCarlo might apply some checks.
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
			if(!operationExecutionsTrace.containsKey(opName)) {
				operationExecutionsTrace.put(opName, 1);
			} else {
				operationExecutionsTrace.computeIfPresent(opName, (key, val) -> val + 1);
			}

			// update enabled operations
			Set<String> enabledOperations = cache.readEnabledOperationsWithCaching(transition.getSource());
			for(String enabledOp : enabledOperations) {
				if(!operationEnablingsTrace.containsKey(enabledOp)) {
					operationEnablingsTrace.put(enabledOp, 1);
				} else {
					operationEnablingsTrace.computeIfPresent(enabledOp, (key, val) -> val + 1);
				}
			}
		}

    	for(String key : operationEnablingsTrace.keySet()) {
    		//update enabled operations for all traces
			int addedEnabling = operationEnablingsTrace.getOrDefault(key, 0);

			if(!operationEnablings.containsKey(key)) {
				operationEnablings.put(key, new ArrayList<>());
			}
			operationEnablings.get(key).add(addedEnabling);

    		//update executed operations for all traces
			int addedExecution = operationExecutionsTrace.getOrDefault(key, 0);
			if(!operationExecutions.containsKey(key)) {
				operationExecutions.put(key, new ArrayList<>());
			}
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

	protected void calculateStatistics() {
		stats = new SimulationStats(this.numberExecutions, this.numberExecutions, 1.0, calculateExtendedStats());
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
}
