package de.prob2.ui.simulation.simulators.check;


import com.google.inject.Injector;
import de.prob.statespace.State;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.simulation.SimulationHelperFunctions;
import de.prob2.ui.simulation.choice.SimulationCheckingType;
import de.prob2.ui.verifications.Checked;

import java.util.Map;

public class SimulationMonteCarloChecker extends SimulationMonteCarlo {

	protected final SimulationCheckingType type;

	protected int numberSuccess;

	public SimulationMonteCarloChecker(Injector injector, CurrentTrace currentTrace, int numberExecutions, int maxStepsBeforeProperty,
									   SimulationCheckingType type, Map<String, Object> additionalInformation) {
		super(injector, currentTrace, numberExecutions, maxStepsBeforeProperty, additionalInformation);
		this.type = type;
		this.numberSuccess = 0;
	}

	@Override
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
			if(i >= startAtStep && !destination.isInvariantOk()) {
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
		String invariant = (String) additionalInformation.get("PREDICATE");
		SimulationHelperFunctions.EvaluationMode mode = SimulationHelperFunctions.extractMode(currentTrace.getModel());
		for(int i = 0; i < trace.getTransitionList().size(); i++) {
			Transition transition = trace.getTransitionList().get(i);
			State destination = transition.getDestination();
			if(destination.isInitialised()) {
				String evalResult = simulationEventHandler.getCache().readValueWithCaching(destination, invariant, mode);
				if (i >= startAtStep && "FALSE".equals(evalResult)) {
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
		String finalPredicate = (String) additionalInformation.get("PREDICATE");
		int size = trace.getTransitionList().size();
		Transition transition = trace.getTransitionList().get(size - 1);
		State destination = transition.getDestination();
		if(destination.isInitialised()) {
			SimulationHelperFunctions.EvaluationMode mode = SimulationHelperFunctions.extractMode(currentTrace.getModel());
			String evalResult = simulationEventHandler.getCache().readValueWithCaching(destination, finalPredicate, mode);
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
		String predicate = (String) additionalInformation.get("PREDICATE");
		SimulationHelperFunctions.EvaluationMode mode = SimulationHelperFunctions.extractMode(currentTrace.getModel());
		for(int i = 0; i < trace.getTransitionList().size(); i++) {
			Transition transition = trace.getTransitionList().get(i);
			State destination = transition.getDestination();
			if(destination.isInitialised()) {
				String evalResult = simulationEventHandler.getCache().readValueWithCaching(destination, predicate, mode);
				if (i >= startAtStep && "TRUE".equals(evalResult)) {
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
		int maximumTime = (int) additionalInformation.get("TIME");
		if(time - startAtTime <= maximumTime) {
			numberSuccess++;
			return Checked.SUCCESS;
		}
		return Checked.FAIL;
	}

}
