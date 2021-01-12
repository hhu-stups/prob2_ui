package de.prob2.ui.simulation.simulators.check;

import de.prob.statespace.Trace;

public class SimulationEstimator extends SimulationMonteCarlo {

    public SimulationEstimator(Trace trace, int numberExecutions, int numberStepsPerExecution) {
        super(trace, numberExecutions, numberStepsPerExecution);
        //TODO
    }

    public void check() {
        // TODO do after run
        // TODO estimate
        // What do we want to estimate? Probability? Integer expressions? Boolean expressions? What are the conditions for estimation? Other complicated types e.g. expressions might be hard
    }

}
