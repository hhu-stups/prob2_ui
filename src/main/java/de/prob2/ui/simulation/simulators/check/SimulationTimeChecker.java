package de.prob2.ui.simulation.simulators.check;

import de.prob.statespace.Trace;
import de.prob2.ui.simulation.simulators.ProbabilityBasedSimulator;

public class SimulationTimeChecker extends ProbabilityBasedSimulator {

    public enum TimeCheckResult {
        NOT_FINISHED, SUCCESS, FAIL
    }

    private Integer result;

    private int targetTime;

    private Trace trace;

    public SimulationTimeChecker(Trace trace, int targetTime) {
        super();
        this.result = null;
        this.trace = trace;
        this.targetTime = targetTime;
    }

    @Override
    public void run() {
        Trace newTrace = setupBeforeSimulation(trace);
        while(!finished) {
            newTrace = simulationStep(newTrace);
        }
    }

    @Override
    protected void finishSimulation() {
        super.finishSimulation();
        this.result = time.get();
    }

    public TimeCheckResult check() {
        if(!finished) {
            return TimeCheckResult.NOT_FINISHED;
        } else {
            if(result <= targetTime) {
                return TimeCheckResult.SUCCESS;
            } else {
                return TimeCheckResult.FAIL;
            }
        }
    }
}
