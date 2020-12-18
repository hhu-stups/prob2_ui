package de.prob2.ui.simulation.simulators;

import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob.statespace.State;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import de.prob2.ui.simulation.configuration.OperationConfiguration;
import de.prob2.ui.simulation.configuration.VariableChoice;
import de.prob2.ui.simulation.configuration.VariableConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
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

    @Override
    public Trace executeNextOperation(OperationConfiguration opConfig, Trace trace) {
        String opName = opConfig.getOpName();
        List<VariableChoice> choices = opConfig.getVariableChoices();
        State currentState = trace.getCurrentState();
        Trace newTrace = trace;
        if(choices == null) {
            List<Transition> transitions = currentState.getTransitions().stream()
                    .filter(trans -> trans.getName().equals(opName))
                    .collect(Collectors.toList());
            if(transitions.size() > 0) {
                Random rand = new Random();
                Transition transition = transitions.get(rand.nextInt(transitions.size()));
                newTrace = newTrace.add(transition);
                delayRemainingTime(opConfig);
            }
        } else {
            State finalCurrentState = newTrace.getCurrentState();
            String predicate = choices.stream()
                    .map(VariableChoice::getChoice)
                    .map(choice -> chooseVariableValues(finalCurrentState, choice))
                    .collect(Collectors.joining(" & "));
            if(finalCurrentState.getStateSpace().isValidOperation(finalCurrentState, opName, predicate)) {
                Transition transition = finalCurrentState.findTransition(opName, predicate);
                newTrace = newTrace.add(transition);
                delayRemainingTime(opConfig);
            }
        }
        return newTrace;
    }
}
