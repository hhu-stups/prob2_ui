package de.prob2.ui.simulation.simulators.check;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.inject.Injector;

import de.prob.statespace.State;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.simulation.SimulationHelperFunctions;
import de.prob2.ui.simulation.choice.SimulationCheckingType;
import de.prob2.ui.simulation.simulators.Simulator;
import de.prob2.ui.verifications.CheckingStatus;

public class SimulationPropertyChecker implements ISimulationPropertyChecker {

	private final ISimulationPropertyChecker hypothesisCheckerOrEstimator;

	private final SimulationCheckingSimulator simulationCheckingSimulator;

	private final CurrentTrace currentTrace;

	private final SimulationCheckingType type;

	private int numberSuccess;

	private final List<Double> estimatedValues;

	public SimulationPropertyChecker(ISimulationPropertyChecker hypothesisCheckerOrEstimator, Injector injector, CurrentTrace currentTrace, CurrentProject currentProject, int numberExecutions, int maxStepsBeforeProperty,
									 SimulationCheckingType type, Map<String, Object> additionalInformation) {
		this.hypothesisCheckerOrEstimator = hypothesisCheckerOrEstimator;
		this.simulationCheckingSimulator = new SimulationCheckingSimulator(injector, currentTrace, currentProject, numberExecutions, maxStepsBeforeProperty, additionalInformation);
		this.currentTrace = currentTrace;
		this.type = type;
		this.numberSuccess = 0;
		this.estimatedValues = new ArrayList<>();
	}

	public CheckingStatus checkTrace(Trace trace, int time) {
		switch (type) {
			case ALL_INVARIANTS:
				return checkAllInvariants(trace);
			case PREDICATE_INVARIANT:
				return checkPredicateInvariant(trace);
			case PREDICATE_FINAL:
				return checkPredicateFinal(trace);
			case PREDICATE_EVENTUALLY:
				return checkPredicateEventually(trace);
			case TIMING:
				return checkTiming(time);
			case AVERAGE:
			case AVERAGE_MEAN_BETWEEN_STEPS:
			case SUM:
			case SUM_MEAN_BETWEEN_STEPS:
			case MINIMUM:
			case MINIMUM_MEAN_BETWEEN_STEPS:
			case MAXIMUM:
			case MAXIMUM_MEAN_BETWEEN_STEPS:
				return estimateValue(trace);
			default:
				break;
		}
		return CheckingStatus.SUCCESS;
	}

	public CheckingStatus estimateValue(Trace trace) {
		switch (type) {
			case AVERAGE:
				return estimateAverage(trace);
			case AVERAGE_MEAN_BETWEEN_STEPS:
				return estimateAverageWithMeanBetweenSteps(trace);
			case SUM:
				return estimateSum(trace);
			case SUM_MEAN_BETWEEN_STEPS:
				return estimateSumWithMeanBetweenSteps(trace);
			case MINIMUM:
				return estimateMinimum(trace);
			case MINIMUM_MEAN_BETWEEN_STEPS:
				return estimateMinimumWithMeanBetweenSteps(trace);
			case MAXIMUM:
				return estimateMaximum(trace);
			case MAXIMUM_MEAN_BETWEEN_STEPS:
				return estimateMaximumWithMeanBetweenSteps(trace);
			default:
				break;
		}
		return CheckingStatus.SUCCESS;
	}

	public CheckingStatus checkAllInvariants(Trace trace) {
		boolean invariantOk = true;
		for(int i = 0; i < trace.getTransitionList().size(); i++) {
			Transition transition = trace.getTransitionList().get(i);
			State destination = transition.getDestination();
			if(i >= simulationCheckingSimulator.getStartAtStep() && !destination.isInvariantOk()) {
				invariantOk = false;
				break;
			}
		}
		if(invariantOk) {
			numberSuccess++;
			return CheckingStatus.SUCCESS;
		}
		return CheckingStatus.FAIL;
	}

	public CheckingStatus checkPredicateInvariant(Trace trace) {
		boolean invariantOk = true;
		String invariant = (String) this.getAdditionalInformation().get("PREDICATE");
		SimulationHelperFunctions.EvaluationMode mode = SimulationHelperFunctions.extractMode(currentTrace.getModel());
		for(int i = 0; i < trace.getTransitionList().size(); i++) {
			Transition transition = trace.getTransitionList().get(i);
			State destination = transition.getDestination();
			if(destination.isInitialised()) {
				String evalResult = simulationCheckingSimulator.getSimulationEventHandler().getCache().readValueWithCaching(destination, simulationCheckingSimulator.getVariables(), invariant, mode);
				if (i >= simulationCheckingSimulator.getStartAtStep() && "FALSE".equals(evalResult)) {
					invariantOk = false;
					break;
				}
			}
		}
		if(invariantOk) {
			numberSuccess++;
			return CheckingStatus.SUCCESS;
		}
		return CheckingStatus.FAIL;
	}

	public CheckingStatus checkPredicateFinal(Trace trace) {
		boolean predicateOk = true;
		String finalPredicate = (String) this.getAdditionalInformation().get("PREDICATE");
		int size = trace.getTransitionList().size();
		Transition transition = trace.getTransitionList().get(size - 1);
		State destination = transition.getDestination();
		if(destination.isInitialised()) {
			SimulationHelperFunctions.EvaluationMode mode = SimulationHelperFunctions.extractMode(currentTrace.getModel());
			String evalResult = simulationCheckingSimulator.getSimulationEventHandler().getCache().readValueWithCaching(destination, simulationCheckingSimulator.getVariables(), finalPredicate, mode);
			if ("FALSE".equals(evalResult)) {
				predicateOk = false;
			}
		}
		if(predicateOk) {
			numberSuccess++;
			return CheckingStatus.SUCCESS;
		}
		return CheckingStatus.FAIL;
	}

	public CheckingStatus checkPredicateEventually(Trace trace) {
		boolean predicateOk = false;
		String predicate = (String) this.getAdditionalInformation().get("PREDICATE");
		SimulationHelperFunctions.EvaluationMode mode = SimulationHelperFunctions.extractMode(currentTrace.getModel());
		for(int i = 0; i < trace.getTransitionList().size(); i++) {
			Transition transition = trace.getTransitionList().get(i);
			State destination = transition.getDestination();
			if(destination.isInitialised()) {
				String evalResult = simulationCheckingSimulator.getSimulationEventHandler().getCache().readValueWithCaching(destination, simulationCheckingSimulator.getVariables(), predicate, mode);
				if (i >= simulationCheckingSimulator.getStartAtStep() && "TRUE".equals(evalResult)) {
					predicateOk = true;
					break;
				}
			}
		}
		if(predicateOk) {
			numberSuccess++;
			return CheckingStatus.SUCCESS;
		}
		return CheckingStatus.FAIL;
	}

	public CheckingStatus checkTiming(int time) {
		int maximumTime = simulationCheckingSimulator.getTiming();
		if(time - simulationCheckingSimulator.getStartAtTime() <= maximumTime) {
			numberSuccess++;
			return CheckingStatus.SUCCESS;
		}
		return CheckingStatus.FAIL;
	}

	public CheckingStatus estimateAverage(Trace trace) {
		double value = 0.0;
		String expression = (String) this.getAdditionalInformation().get("EXPRESSION");
		double desiredValue = (double) this.getAdditionalInformation().get("DESIRED_VALUE");
		double epsilon = (double) this.getAdditionalInformation().get("EPSILON");
		SimulationHelperFunctions.EvaluationMode mode = SimulationHelperFunctions.extractMode(currentTrace.getModel());
		int steps = 0;
		for(int i = 0; i < trace.getTransitionList().size(); i++) {
			Transition transition = trace.getTransitionList().get(i);
			State destination = transition.getDestination();
			if(destination.isInitialised()) {
				String evalResult = simulationCheckingSimulator.getSimulationEventHandler().getCache().readValueWithCaching(destination, simulationCheckingSimulator.getVariables(), expression, mode);
				if (i >= simulationCheckingSimulator.getStartAtStep()) {
					value = value + Double.parseDouble(evalResult);
					steps++;
				}
			}
		}
		double res = steps == 0 ? 0.0 : value / steps;

		boolean success = ((SimulationEstimator) hypothesisCheckerOrEstimator).check(res, desiredValue, epsilon);
		if(success) {
			numberSuccess++;
		}

		estimatedValues.add(res);
		return success ? CheckingStatus.SUCCESS : CheckingStatus.FAIL;
	}

	public CheckingStatus estimateAverageWithMeanBetweenSteps(Trace trace) {
		double value = 0.0;
		String expression = (String) this.getAdditionalInformation().get("EXPRESSION");
		double desiredValue = (double) this.getAdditionalInformation().get("DESIRED_VALUE");
		double epsilon = (double) this.getAdditionalInformation().get("EPSILON");
		SimulationHelperFunctions.EvaluationMode mode = SimulationHelperFunctions.extractMode(currentTrace.getModel());
		int steps = 0;
		for(int i = 0; i < trace.getTransitionList().size(); i++) {
			Transition transition = trace.getTransitionList().get(i);
			State startState = transition.getSource();
			State endState = transition.getDestination();
			if(startState.isInitialised()) {
				String evalStartResult = simulationCheckingSimulator.getSimulationEventHandler().getCache().readValueWithCaching(startState, simulationCheckingSimulator.getVariables(), expression, mode);
				String evalEndResult = simulationCheckingSimulator.getSimulationEventHandler().getCache().readValueWithCaching(endState, simulationCheckingSimulator.getVariables(), expression, mode);
				if (i >= simulationCheckingSimulator.getStartAtStep()) {
					value = value + (Double.parseDouble(evalStartResult) + Double.parseDouble(evalEndResult))/2.0;
					steps++;
				}
			}
		}
		double res = steps == 0 ? 0.0 : value / steps;

		boolean success = ((SimulationEstimator) hypothesisCheckerOrEstimator).check(res, desiredValue, epsilon);
		if(success) {
			numberSuccess++;
		}

		estimatedValues.add(res);
		return success ? CheckingStatus.SUCCESS : CheckingStatus.FAIL;
	}

	public CheckingStatus estimateSum(Trace trace) {
		double value = 0.0;
		String expression = (String) this.getAdditionalInformation().get("EXPRESSION");
		double desiredValue = (double) this.getAdditionalInformation().get("DESIRED_VALUE");
		double epsilon = (double) this.getAdditionalInformation().get("EPSILON");

		SimulationHelperFunctions.EvaluationMode mode = SimulationHelperFunctions.extractMode(currentTrace.getModel());
		for(int i = 0; i < trace.getTransitionList().size(); i++) {
			Transition transition = trace.getTransitionList().get(i);
			State destination = transition.getDestination();
			if(destination.isInitialised()) {
				String evalResult = simulationCheckingSimulator.getSimulationEventHandler().getCache().readValueWithCaching(destination, simulationCheckingSimulator.getVariables(), expression, mode);
				if (i >= simulationCheckingSimulator.getStartAtStep()) {
					value = value + Double.parseDouble(evalResult);
				}
			}
		}

		boolean success = ((SimulationEstimator) hypothesisCheckerOrEstimator).check(value, desiredValue, epsilon);
		if(success) {
			numberSuccess++;
		}

		estimatedValues.add(value);
		return success ? CheckingStatus.SUCCESS : CheckingStatus.FAIL;
	}

	public CheckingStatus estimateSumWithMeanBetweenSteps(Trace trace) {
		double value = 0.0;
		String expression = (String) this.getAdditionalInformation().get("EXPRESSION");
		double desiredValue = (double) this.getAdditionalInformation().get("DESIRED_VALUE");
		double epsilon = (double) this.getAdditionalInformation().get("EPSILON");

		SimulationHelperFunctions.EvaluationMode mode = SimulationHelperFunctions.extractMode(currentTrace.getModel());
		for(int i = 0; i < trace.getTransitionList().size(); i++) {
			Transition transition = trace.getTransitionList().get(i);
			State startState = transition.getSource();
			State endState = transition.getDestination();
			if(startState.isInitialised()) {
				String evalStartResult = simulationCheckingSimulator.getSimulationEventHandler().getCache().readValueWithCaching(startState, simulationCheckingSimulator.getVariables(), expression, mode);
				String evalEndResult = simulationCheckingSimulator.getSimulationEventHandler().getCache().readValueWithCaching(endState, simulationCheckingSimulator.getVariables(), expression, mode);
				if (i >= simulationCheckingSimulator.getStartAtStep()) {
					value = value + (Double.parseDouble(evalStartResult) + Double.parseDouble(evalEndResult))/2.0;
				}
			}
		}

		boolean success = ((SimulationEstimator) hypothesisCheckerOrEstimator).check(value, desiredValue, epsilon);
		if(success) {
			numberSuccess++;
		}

		estimatedValues.add(value);
		return success ? CheckingStatus.SUCCESS : CheckingStatus.FAIL;
	}

	public CheckingStatus estimateMinimum(Trace trace) {
		double value = Double.MAX_VALUE;
		String expression = (String) this.getAdditionalInformation().get("EXPRESSION");
		double desiredValue = (double) this.getAdditionalInformation().get("DESIRED_VALUE");
		double epsilon = (double) this.getAdditionalInformation().get("EPSILON");
		SimulationHelperFunctions.EvaluationMode mode = SimulationHelperFunctions.extractMode(currentTrace.getModel());
		int steps = 0;
		for(int i = 0; i < trace.getTransitionList().size(); i++) {
			Transition transition = trace.getTransitionList().get(i);
			State destination = transition.getDestination();
			if(destination.isInitialised()) {
				String evalResult = simulationCheckingSimulator.getSimulationEventHandler().getCache().readValueWithCaching(destination, simulationCheckingSimulator.getVariables(), expression, mode);
				if (i >= simulationCheckingSimulator.getStartAtStep()) {
					value = Math.min(Double.parseDouble(evalResult), value);
					steps++;
				}
			}
		}
		double res = steps == 0 ? Double.MAX_VALUE : value;

		boolean success = ((SimulationEstimator) hypothesisCheckerOrEstimator).check(res, desiredValue, epsilon);
		if(success) {
			numberSuccess++;
		}

		estimatedValues.add(res);
		return success ? CheckingStatus.SUCCESS : CheckingStatus.FAIL;
	}

	public CheckingStatus estimateMinimumWithMeanBetweenSteps(Trace trace) {
		double value = Double.MAX_VALUE;
		String expression = (String) this.getAdditionalInformation().get("EXPRESSION");
		double desiredValue = (double) this.getAdditionalInformation().get("DESIRED_VALUE");
		double epsilon = (double) this.getAdditionalInformation().get("EPSILON");
		SimulationHelperFunctions.EvaluationMode mode = SimulationHelperFunctions.extractMode(currentTrace.getModel());
		int steps = 0;
		for(int i = 0; i < trace.getTransitionList().size(); i++) {
			Transition transition = trace.getTransitionList().get(i);
			State startState = transition.getSource();
			State endState = transition.getDestination();
			if(startState.isInitialised()) {
				String evalStartResult = simulationCheckingSimulator.getSimulationEventHandler().getCache().readValueWithCaching(startState, simulationCheckingSimulator.getVariables(), expression, mode);
				String evalEndResult = simulationCheckingSimulator.getSimulationEventHandler().getCache().readValueWithCaching(endState, simulationCheckingSimulator.getVariables(), expression, mode);
				if (i >= simulationCheckingSimulator.getStartAtStep()) {
					value = Math.min(value, (Double.parseDouble(evalStartResult) + Double.parseDouble(evalEndResult))/2.0);
					steps++;
				}
			}
		}
		double res = steps == 0 ? Double.MAX_VALUE : value;

		boolean success = ((SimulationEstimator) hypothesisCheckerOrEstimator).check(res, desiredValue, epsilon);
		if(success) {
			numberSuccess++;
		}

		estimatedValues.add(res);
		return success ? CheckingStatus.SUCCESS : CheckingStatus.FAIL;
	}

	public CheckingStatus estimateMaximum(Trace trace) {
		double value = Double.MIN_VALUE;
		String expression = (String) this.getAdditionalInformation().get("EXPRESSION");
		double desiredValue = (double) this.getAdditionalInformation().get("DESIRED_VALUE");
		double epsilon = (double) this.getAdditionalInformation().get("EPSILON");
		SimulationHelperFunctions.EvaluationMode mode = SimulationHelperFunctions.extractMode(currentTrace.getModel());
		int steps = 0;
		for(int i = 0; i < trace.getTransitionList().size(); i++) {
			Transition transition = trace.getTransitionList().get(i);
			State destination = transition.getDestination();
			if(destination.isInitialised()) {
				String evalResult = simulationCheckingSimulator.getSimulationEventHandler().getCache().readValueWithCaching(destination, simulationCheckingSimulator.getVariables(), expression, mode);
				if (i >= simulationCheckingSimulator.getStartAtStep()) {
					value = Math.max(Double.parseDouble(evalResult), value);
					steps++;
				}
			}
		}
		double res = steps == 0 ? Double.MIN_VALUE : value;

		boolean success = ((SimulationEstimator) hypothesisCheckerOrEstimator).check(res, desiredValue, epsilon);
		if(success) {
			numberSuccess++;
		}

		estimatedValues.add(res);
		return success ? CheckingStatus.SUCCESS : CheckingStatus.FAIL;
	}

	public CheckingStatus estimateMaximumWithMeanBetweenSteps(Trace trace) {
		double value = Double.MIN_VALUE;
		String expression = (String) this.getAdditionalInformation().get("EXPRESSION");
		double desiredValue = (double) this.getAdditionalInformation().get("DESIRED_VALUE");
		double epsilon = (double) this.getAdditionalInformation().get("EPSILON");
		SimulationHelperFunctions.EvaluationMode mode = SimulationHelperFunctions.extractMode(currentTrace.getModel());
		int steps = 0;
		for(int i = 0; i < trace.getTransitionList().size(); i++) {
			Transition transition = trace.getTransitionList().get(i);
			State startState = transition.getSource();
			State endState = transition.getDestination();
			if(startState.isInitialised()) {
				String evalStartResult = simulationCheckingSimulator.getSimulationEventHandler().getCache().readValueWithCaching(startState, simulationCheckingSimulator.getVariables(), expression, mode);
				String evalEndResult = simulationCheckingSimulator.getSimulationEventHandler().getCache().readValueWithCaching(endState, simulationCheckingSimulator.getVariables(), expression, mode);
				if (i >= simulationCheckingSimulator.getStartAtStep()) {
					value = Math.max(value, (Double.parseDouble(evalStartResult) + Double.parseDouble(evalEndResult))/2.0);
					steps++;
				}
			}
		}
		double res = steps == 0 ? Double.MIN_VALUE : value;

		boolean success = ((SimulationEstimator) hypothesisCheckerOrEstimator).check(res, desiredValue, epsilon);
		if(success) {
			numberSuccess++;
		}

		estimatedValues.add(res);
		return success ? CheckingStatus.SUCCESS : CheckingStatus.FAIL;
	}

	@Override
	public Map<String, List<Integer>> getOperationExecutions() {
		return simulationCheckingSimulator.getOperationExecutions();
	}

	@Override
	public List<Trace> getResultingTraces() {
		return simulationCheckingSimulator.getResultingTraces();
	}

	@Override
	public List<List<Integer>> getResultingTimestamps() {
		return simulationCheckingSimulator.getResultingTimestamps();
	}

	@Override
	public List<CheckingStatus> getResultingStatus() {
		return simulationCheckingSimulator.getResultingStatus();
	}

	@Override
	public SimulationStats getStats() {
		return simulationCheckingSimulator.getStats();
	}

	public List<Double> getEstimatedValues() {
		return estimatedValues;
	}

	@Override
	public void setStats(SimulationStats stats) {
		simulationCheckingSimulator.setStats(stats);
	}

	public Map<String, Object> getAdditionalInformation() {
		return simulationCheckingSimulator.getAdditionalInformation();
	}

	@Override
	public SimulationCheckingSimulator.MonteCarloCheckResult getResult() {
		return simulationCheckingSimulator.getResult();
	}

	@Override
	public void setResult(SimulationCheckingSimulator.MonteCarloCheckResult result) {
		simulationCheckingSimulator.setResult(result);
	}

	@Override
	public int getNumberSuccess() {
		return numberSuccess;
	}

	@Override
	public SimulationExtendedStats calculateExtendedStats() {
		return simulationCheckingSimulator.calculateExtendedStats();
	}

	public void calculateStatistics(long time) {
		simulationCheckingSimulator.calculateStatistics(time);
	}

	@Override
	public void run() {
		simulationCheckingSimulator.run(this);
	}

	public void run(ISimulationPropertyChecker simulationPropertyChecker) {
		simulationCheckingSimulator.run(simulationPropertyChecker);
	}

	@Override
	public void check() {
		simulationCheckingSimulator.check();
	}

	public Simulator getSimulator() {
		return simulationCheckingSimulator;
	}
}
