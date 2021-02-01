package de.prob2.ui.simulation.simulators.check;

import de.prob.animator.command.ExecuteOperationException;
import de.prob.check.tracereplay.ITraceChecker;
import de.prob.statespace.Trace;
import de.prob2.ui.animation.tracereplay.ReplayTrace;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.simulation.simulators.AbstractTraceSimulator;

import java.util.Map;

public class SimulationTraceChecker extends AbstractTraceSimulator implements ITraceChecker {

    public enum TraceCheckResult {
        NOT_FINISHED, SUCCESS, FAIL
    }

    private TraceCheckResult result;

    private final Map<String, Object> additionalInformation;

    private Trace resultingTrace;

    public SimulationTraceChecker(final CurrentTrace currentTrace, Trace trace, ReplayTrace replayTrace, Map<String, Object> additionalInformation) {
        super(currentTrace, trace, replayTrace);
        this.result = TraceCheckResult.NOT_FINISHED;
        this.additionalInformation = additionalInformation;
        this.resultingTrace = null;
    }

    @Override
    public void run() {
        this.counter = 0;
        try {
            Trace newTrace = setupBeforeSimulation(trace);
            while(!endingConditionReached(newTrace)) {
                newTrace = simulationStep(newTrace);
            }
            this.resultingTrace = newTrace;
        } catch (ExecuteOperationException e) {
            System.out.println("TRACE REPLAY IN SIMULATION ERROR");
        }
    }

    public TraceCheckResult check() {
        if(counter == persistentTrace.getTransitionList().size()) {
            if(additionalInformation.containsKey("TIME")) {
                int time = (int) additionalInformation.get("TIME");
                this.result = this.time.get() <= time ? TraceCheckResult.SUCCESS : TraceCheckResult.FAIL;
            } else {
                this.result = TraceCheckResult.SUCCESS;
            }
        } else {
            this.result = TraceCheckResult.FAIL;
        }
        return result;
    }

    public Trace getResultingTrace() {
        return resultingTrace;
    }
}
