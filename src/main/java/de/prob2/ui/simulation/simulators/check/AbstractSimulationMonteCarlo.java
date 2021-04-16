package de.prob2.ui.simulation.simulators.check;


import com.google.inject.Injector;
import de.prob.statespace.State;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.simulation.choice.SimulationCheckingType;

import java.util.Map;

public class AbstractSimulationMonteCarlo extends SimulationMonteCarlo {

	protected final SimulationCheckingType type;

	protected int numberSuccess;

	public AbstractSimulationMonteCarlo(Injector injector, CurrentTrace currentTrace, int numberExecutions, SimulationCheckingType type, Map<String, Object> additionalInformation) {
		super(injector, currentTrace, numberExecutions, additionalInformation);
		this.type = type;
		this.numberSuccess = 0;
	}

	@Override
	public void checkTrace(Trace trace, int time) {
		switch (type) {
			case ALL_INVARIANTS:
				checkAllInvariants(trace);
				break;
			case PREDICATE_INVARIANT:
				checkPredicateInvariant(trace);
				break;
			case PREDICATE_FINAL:
				checkPredicateFinal(trace);
				break;
			case PREDICATE_EVENTUALLY:
				checkPredicateEventually(trace);
				break;
			case TIMING:
				checkTiming(time);
				break;
			default:
				break;
		}
	}

	public void checkAllInvariants(Trace trace) {
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
		}
	}

	public void checkPredicateInvariant(Trace trace) {
		boolean invariantOk = true;
		String invariant = (String) additionalInformation.get("PREDICATE");
		for(int i = 0; i < trace.getTransitionList().size(); i++) {
			Transition transition = trace.getTransitionList().get(i);
			State destination = transition.getDestination();
			if(destination.isInitialised()) {
				String evalResult = simulationEventHandler.getCache().readValueWithCaching(destination, invariant);
				if (i >= startAtStep && "FALSE".equals(evalResult)) {
					invariantOk = false;
					break;
				}
			}
		}
		if(invariantOk) {
			numberSuccess++;
		}
	}

	public void checkPredicateFinal(Trace trace) {
		boolean predicateOk = true;
		String finalPredicate = (String) additionalInformation.get("PREDICATE");
		int size = trace.getTransitionList().size();
		Transition transition = trace.getTransitionList().get(size - 1);
		State destination = transition.getDestination();
		if(destination.isInitialised()) {
			String evalResult = simulationEventHandler.getCache().readValueWithCaching(destination, finalPredicate);
			if ("FALSE".equals(evalResult)) {
				predicateOk = false;
			}
		}
		if(predicateOk) {
			numberSuccess++;
		}
	}

	public void checkPredicateEventually(Trace trace) {
		boolean predicateOk = false;
		String predicate = (String) additionalInformation.get("PREDICATE");
		for(int i = 0; i < trace.getTransitionList().size(); i++) {
			Transition transition = trace.getTransitionList().get(i);
			State destination = transition.getDestination();
			if(destination.isInitialised()) {
				String evalResult = simulationEventHandler.getCache().readValueWithCaching(destination, predicate);
				if (i >= startAtStep && "TRUE".equals(evalResult)) {
					predicateOk = true;
					break;
				}
			}
		}
		if(predicateOk) {
			numberSuccess++;
		}
	}

	public void checkTiming(int time) {
		int maximumTime = (int) additionalInformation.get("TIME");
		if(time - startAtTime <= maximumTime) {
			numberSuccess++;
		}
	}

}
