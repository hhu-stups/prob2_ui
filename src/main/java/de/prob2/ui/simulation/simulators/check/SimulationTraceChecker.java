package de.prob2.ui.simulation.simulators.check;

import de.prob.animator.command.ExecuteOperationException;
import de.prob.check.tracereplay.ITraceChecker;
import de.prob.statespace.Trace;
import de.prob2.ui.animation.tracereplay.ReplayTrace;
import de.prob2.ui.simulation.simulators.AbstractTraceSimulator;

public class SimulationTraceChecker extends AbstractTraceSimulator implements ITraceChecker {

    public enum TraceCheckResult {
        NOT_FINISHED, SUCCESS, FAIL
    }

    private TraceCheckResult result;

    public SimulationTraceChecker(Trace trace, ReplayTrace replayTrace) {
        super(trace, replayTrace);
        this.result = TraceCheckResult.NOT_FINISHED;
    }

    @Override
    public void run() {
        try {
            Trace newTrace = setupBeforeSimulation(trace);
            while(!finished && counter < replayTrace.getPersistentTrace().getTransitionList().size()) {
                newTrace = simulationStep(newTrace);
            }
        } catch (ExecuteOperationException e) {
            System.out.println("TRACE REPLAY IN SIMULATION ERROR");
        }
    }

    public TraceCheckResult check() {
        if(counter == replayTrace.getPersistentTrace().getTransitionList().size()) {
            this.result = TraceCheckResult.SUCCESS;
        } else {
            this.result = TraceCheckResult.FAIL;
        }
        return result;
    }
}
