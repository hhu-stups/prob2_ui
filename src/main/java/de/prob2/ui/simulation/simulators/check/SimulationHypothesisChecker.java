package de.prob2.ui.simulation.simulators.check;

import de.prob.statespace.Trace;
import de.prob2.ui.simulation.simulators.ProbabilityBasedSimulator;

import java.util.ArrayList;
import java.util.List;

public class SimulationHypothesisChecker extends SimulationMonteCarlo {

    public enum HypothesisCheckResult {
        NOT_FINISHED, SUCCESS, FAIL
    }

    private double faultTolerance;

    public SimulationHypothesisChecker(Trace trace, int numberExecutions, int numberStepsPerExecution) {
        super(trace, numberExecutions, numberStepsPerExecution);
        //TODO
        this.faultTolerance = 0.0;
    }

    public HypothesisCheckResult check() {
        // TODO do after run
        // TODO Check hypothesis
        // Distinguish fault tolerance for traces and for states
        return HypothesisCheckResult.SUCCESS;
    }

}
