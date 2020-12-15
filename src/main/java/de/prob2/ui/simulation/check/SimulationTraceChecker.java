package de.prob2.ui.simulation.check;

import de.prob.statespace.Trace;
import de.prob2.ui.simulation.AbstractSimulator;
import de.prob2.ui.simulation.configuration.OperationConfiguration;

public class SimulationTraceChecker extends AbstractSimulator {

    private final Trace trace;

    public SimulationTraceChecker(Trace trace) {
        this.trace = trace;
    }

    @Override
    public void run() {
        Trace newTrace = setupBeforeSimulation(trace);
        while(!finished) {
            newTrace = simulationStep(newTrace);
        }
    }

    @Override
    protected boolean chooseNextOperation(OperationConfiguration opConfig, Trace trace) {
        return false;
    }

}
