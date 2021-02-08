package de.prob2.ui.simulation.simulators.check;

import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.statespace.State;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.simulation.choice.SimulationCheckingType;

import java.util.List;
import java.util.Map;

public class AbstractSimulationMonteCarlo extends SimulationMonteCarlo {

    protected final SimulationCheckingType type;

    protected int numberSuccess;

    public AbstractSimulationMonteCarlo(CurrentTrace currentTrace, Trace trace, int numberExecutions, SimulationCheckingType type, Map<String, Object> additionalInformation) {
        super(currentTrace, trace, numberExecutions, additionalInformation);
        this.type = type;
        this.numberSuccess = 0;
    }

    @Override
    public void checkTrace(Trace trace, int time) {
        switch (type) {
            case ALL_INVARIANTS:
                checkAllInvariants(trace);
                break;
            case INVARIANT:
                checkInvariant(trace);
                break;
            case ALMOST_CERTAIN_PROPERTY:
                checkAlmostCertainProperty(trace);
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
        for(Transition transition : trace.getTransitionList()) {
            State destination = transition.getDestination();
            if(!destination.isInvariantOk()) {
                invariantOk = false;
                break;
            }
        }
        if(invariantOk) {
            numberSuccess++;
        }
    }

    public void checkInvariant(Trace trace) {
        boolean invariantOk = true;
        String invariant = (String) additionalInformation.get("INVARIANT");
        for(Transition transition : trace.getTransitionList()) {
            State destination = transition.getDestination();
            if(destination.isInitialised()) {
                AbstractEvalResult evalResult = destination.eval(invariant, FormulaExpand.TRUNCATE);
                if ("FALSE".equals(evalResult.toString())) {
                    invariantOk = false;
                    break;
                }
            }
        }
        if(invariantOk) {
            numberSuccess++;
        }
    }

    public void checkAlmostCertainProperty(Trace trace) {
        // TODO: Fix Property
        String property = (String) additionalInformation.get("PROPERTY");
        for(Transition transition : trace.getTransitionList()) {
            State destination = transition.getDestination();
            if(destination.isInitialised()) {
                AbstractEvalResult evalResult = destination.eval(property, FormulaExpand.TRUNCATE);
                if("TRUE".equals(evalResult.toString())) {
                    List<Transition> transitions = destination.getOutTransitions();
                    String stateID = destination.getId();
                    //TODO: Implement some caching
                    boolean propertyOk = transitions.stream()
                            .map(Transition::getDestination)
                            .map(dest -> stateID.equals(dest.getId()))
                            .reduce(true, (e, a) -> e && a);
                    if(propertyOk) {
                        numberSuccess++;
                        break;
                    }
                }
            }
        }
    }

    public void checkTiming(int time) {
        int maximumTime = (int) additionalInformation.get("TIME");
        if(time <= maximumTime) {
            numberSuccess++;
        }
    }

}
