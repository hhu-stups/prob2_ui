package de.prob2.ui.simulation.check;

import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob.statespace.State;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import de.prob2.ui.simulation.ProbabilityBasedSimulator;
import de.prob2.ui.simulation.configuration.OperationConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

    public HypothesisCheckResult check() {
        // TODO Check hypothesis
        // Distinguish fault tolerance for traces and for states
        return HypothesisCheckResult.SUCCESS;
    }

}
