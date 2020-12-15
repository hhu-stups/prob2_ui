package de.prob2.ui.simulation.check;

import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob.statespace.State;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import de.prob2.ui.simulation.ProbabilityBasedSimulator;
import de.prob2.ui.simulation.configuration.OperationConfiguration;

import java.util.List;
import java.util.stream.Collectors;

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

    protected boolean chooseNextOperation(OperationConfiguration opConfig, Trace trace) {
        String opName = opConfig.getOpName();
        State currentState = trace.getCurrentState();

        double ranDouble = Math.random();
        AbstractEvalResult evalResult = evaluateForSimulation(currentState, opConfig.getProbability());

        List<String> enabledOperations = trace.getNextTransitions().stream()
                .map(Transition::getName)
                .collect(Collectors.toList());
        return Double.parseDouble(evalResult.toString()) > ranDouble && enabledOperations.contains(opName);
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
