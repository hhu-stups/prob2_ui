package de.prob2.ui.simulation;

import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob.statespace.State;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import de.prob2.ui.simulation.configuration.OperationConfiguration;
import de.prob2.ui.simulation.configuration.VariableConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class ProbabilityBasedSimulator extends AbstractSimulator {

    @Override
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
    public String chooseVariableValues(State currentState, List<VariableConfiguration> choice) {
        double ranDouble = Math.random();
        double minimumProbability = 0.0;
        VariableConfiguration chosenConfiguration = choice.get(0);

        //Choose configuration for execution
        for(VariableConfiguration config : choice) {
            AbstractEvalResult probabilityResult = evaluateForSimulation(currentState, config.getProbability());
            minimumProbability += Double.parseDouble(probabilityResult.toString());
            chosenConfiguration = config;
            if(minimumProbability > ranDouble) {
                break;
            }
        }

        Map<String, String> chosenValues = chosenConfiguration.getValues();
        List<String> conjuncts = new ArrayList<>();
        for(String key : chosenValues.keySet()) {
            AbstractEvalResult evalResult = evaluateForSimulation(currentState, chosenValues.get(key));
            conjuncts.add(key + " = " + evalResult.toString());
        }
        return String.join(" & ", conjuncts);
    }

}
