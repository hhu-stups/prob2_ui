package de.prob2.ui.simulation.check;

import de.prob.statespace.Trace;
import de.prob2.ui.simulation.AbstractSimulator;

import java.util.ArrayList;
import java.util.List;

public class SimulationHypothesisChecker extends AbstractSimulator {

    public enum HypothesisCheckResult {
        NOT_FINISHED, SUCCESS, FAIL
    }

    private List<Trace> result;

    private Trace trace;

    private int numberExecutions;

    private int numberStepsPerExecutions;

    private double faultTolerance;

    public SimulationHypothesisChecker(Trace trace, int numberExecutions, int numberStepsPerExecution) {
        super();
        this.result = new ArrayList<>();
        this.trace = trace;
        this.numberExecutions = numberExecutions;
        this.numberStepsPerExecutions = numberStepsPerExecution;
        //TODO
        this.faultTolerance = 0.0;
    }

    @Override
    public void run() {
        Trace startTrace = setupBeforeSimulation(trace);
        for(int i = 0; i < numberExecutions; i++) {
            Trace newTrace = startTrace;
            int stepCounter = 0;
            this.finished = false;
            while(stepCounter < numberStepsPerExecutions && !finished) {
                Trace nextTrace = simulationStep(newTrace);
                stepCounter += nextTrace.getTransitionList().size() - newTrace.getTransitionList().size();
                newTrace = nextTrace;
                Trace addedTrace = newTrace.gotoPosition(newTrace.getTransitionList().size() - 1 - (stepCounter - numberStepsPerExecutions));
                result.add(addedTrace);
            }
        }
    }

    public HypothesisCheckResult check() {
        // TODO Check hypothesis
        // Distinguish fault tolerance for traces and for states
        return HypothesisCheckResult.SUCCESS;
    }

}
