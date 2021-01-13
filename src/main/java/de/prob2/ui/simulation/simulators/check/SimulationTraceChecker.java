package de.prob2.ui.simulation.simulators.check;

import de.prob.animator.command.ExecuteOperationException;
import de.prob.check.tracereplay.ITraceChecker;
import de.prob.statespace.Trace;
import de.prob2.ui.animation.tracereplay.ReplayTrace;
import de.prob2.ui.simulation.simulators.AbstractTraceSimulator;

import java.util.Map;

public class SimulationTraceChecker extends AbstractTraceSimulator implements ITraceChecker {

    public enum TraceCheckResult {
        NOT_FINISHED, SUCCESS, FAIL
    }

    private TraceCheckResult result;

    private final Map<String, Object> additionalInformation;

    public SimulationTraceChecker(Trace trace, ReplayTrace replayTrace, Map<String, Object> additionalInformation) {
        super(trace, replayTrace);
        this.result = TraceCheckResult.NOT_FINISHED;
        this.additionalInformation = additionalInformation;
    }

    @Override
    public void run() {
        this.counter = 0;
        try {
            Trace newTrace = setupBeforeSimulation(trace);
            while(!finished) {
                newTrace = simulationStep(newTrace);
                if(endingConditionReached(newTrace)) {
                    finishSimulation();
                }
            }
        } catch (ExecuteOperationException e) {
            System.out.println("TRACE REPLAY IN SIMULATION ERROR");
        }
    }

    public TraceCheckResult check() {
        if(counter == replayTrace.getPersistentTrace().getTransitionList().size()) {
            if(additionalInformation.containsKey("TIME")) {
                int time = (int) additionalInformation.get("TIME");
                this.result = this.time.get() <= time ? TraceCheckResult.SUCCESS : TraceCheckResult.FAIL;
                System.out.println(this.time.get());
                System.out.println("TIME");
            } else {
                this.result = TraceCheckResult.SUCCESS;
            }
        } else {
            this.result = TraceCheckResult.FAIL;
        }
        return result;
    }
}
