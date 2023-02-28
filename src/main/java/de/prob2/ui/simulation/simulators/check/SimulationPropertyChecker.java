package de.prob2.ui.simulation.simulators.check;


import com.google.inject.Injector;
import de.prob.statespace.State;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.simulation.SimulationHelperFunctions;
import de.prob2.ui.simulation.choice.SimulationCheckingType;
import de.prob2.ui.simulation.simulators.Simulator;
import de.prob2.ui.verifications.Checked;

import java.util.List;
import java.util.Map;

public class SimulationPropertyChecker implements ISimulationPropertyChecker {

	private final SimulationCheckingSimulator simulationCheckingSimulator;

	private final CurrentTrace currentTrace;

	private final SimulationCheckingType type;

	private int numberSuccess;

	public SimulationPropertyChecker(Injector injector, CurrentTrace currentTrace, int numberExecutions, int maxStepsBeforeProperty,
									 SimulationCheckingType type, Map<String, Object> additionalInformation) {
		this.simulationCheckingSimulator = new SimulationCheckingSimulator(injector, currentTrace, numberExecutions, maxStepsBeforeProperty, additionalInformation);
		this.currentTrace = currentTrace;
		this.type = type;
		this.numberSuccess = 0;
	}

	public Checked checkTrace(Trace trace, int time) {
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
			default:
				break;
		}
		return Checked.SUCCESS;
	}

	public Checked checkAllInvariants(Trace trace) {
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
			return Checked.SUCCESS;
		}
		return Checked.FAIL;
	}

	public Checked checkPredicateInvariant(Trace trace) {
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
			return Checked.SUCCESS;
		}
		return Checked.FAIL;
	}

	public Checked checkPredicateFinal(Trace trace) {
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
			return Checked.SUCCESS;
		}
		return Checked.FAIL;
	}

	public Checked checkPredicateEventually(Trace trace) {
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
			return Checked.SUCCESS;
		}
		return Checked.FAIL;
	}

	public Checked checkTiming(int time) {
		int maximumTime = (int) this.getAdditionalInformation().get("TIME");
		if(time - simulationCheckingSimulator.getStartAtTime() <= maximumTime) {
			numberSuccess++;
			return Checked.SUCCESS;
		}
		return Checked.FAIL;
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
	public List<Checked> getResultingStatus() {
		return simulationCheckingSimulator.getResultingStatus();
	}

	@Override
	public SimulationStats getStats() {
		return simulationCheckingSimulator.getStats();
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