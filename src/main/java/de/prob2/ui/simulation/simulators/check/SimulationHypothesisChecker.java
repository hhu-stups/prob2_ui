package de.prob2.ui.simulation.simulators.check;

import de.prob.statespace.Trace;
import de.prob2.ui.simulation.simulators.ProbabilityBasedSimulator;

import java.util.ArrayList;
import java.util.List;

public class SimulationHypothesisChecker extends ProbabilityBasedSimulator {

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
        Thread thread = new Thread(() -> {
            Trace startTrace = setupBeforeSimulation(trace);
            try {
                startTrace.getStateSpace().startTransaction();
                for (int i = 0; i < numberExecutions; i++) {
                    Trace newTrace = startTrace;
                    int stepCounter = 0;
                    this.finished = false;
                    while (stepCounter < numberStepsPerExecutions && !finished) {
                        Trace nextTrace = simulationStep(newTrace);
                        stepCounter += nextTrace.getTransitionList().size() - newTrace.getTransitionList().size();
                        newTrace = nextTrace;
                        if(stepCounter >= numberStepsPerExecutions) {
                            Trace addedTrace = new Trace(newTrace.getStateSpace());
                            addedTrace.addTransitions(newTrace.getTransitionList().subList(0, numberStepsPerExecutions));
                            result.add(addedTrace);
                        }
                    }
                }
            } finally {
                startTrace.getStateSpace().endTransaction();
            }
        });
        thread.start();

    }

    public HypothesisCheckResult check() {
        // TODO do after run
        // TODO Check hypothesis
        // Distinguish fault tolerance for traces and for states
        return HypothesisCheckResult.SUCCESS;
    }

}
