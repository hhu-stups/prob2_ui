package de.prob2.ui.simulation.simulators;

import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob.statespace.State;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import de.prob2.ui.simulation.configuration.OperationConfiguration;
import de.prob2.ui.simulation.configuration.VariableChoice;
import de.prob2.ui.simulation.configuration.VariableConfiguration;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

public abstract class ProbabilityBasedSimulator extends AbstractSimulator {

    private static final Map<String, Map<String, Double>> probabilityCache = new HashMap<>();

    @Override
    protected boolean chooseNextOperation(OperationConfiguration opConfig, Trace trace) {
        State currentState = trace.getCurrentState();
        double ranDouble = Math.random();
        String stateID = currentState.getId();
        String opName = opConfig.getOpName();
        double evalProbability = -1.0;
        if(probabilityCache.containsKey(stateID)) {
            if(probabilityCache.get(stateID).containsKey(opName)) {
                evalProbability = probabilityCache.get(stateID).get(opName);
            } else {
                AbstractEvalResult evalResult = evaluateForSimulation(currentState, opConfig.getProbability());
                evalProbability = Double.parseDouble(evalResult.toString());
                probabilityCache.get(stateID).put(opName, evalProbability);
            }
        } else {
            probabilityCache.put(stateID, new HashMap<>());
            AbstractEvalResult evalResult = evaluateForSimulation(currentState, opConfig.getProbability());
            evalProbability = Double.parseDouble(evalResult.toString());
            probabilityCache.get(stateID).put(opName, evalProbability);
        }

        return evalProbability > ranDouble;
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
        StringBuilder conjuncts = new StringBuilder();
        for(Iterator<String> it = chosenValues.keySet().iterator(); it.hasNext(); ) {
            String next = it.next();
            AbstractEvalResult evalResult = evaluateForSimulation(currentState, chosenValues.get(next));
            conjuncts.append(next);
            conjuncts.append(" = ");
            conjuncts.append(evalResult.toString());
            if(it.hasNext()) {
                conjuncts.append(" & ");
            }
        }
        return conjuncts.toString();
    }

    @Override
    public Trace executeNextOperation(OperationConfiguration opConfig, Trace trace) {
        String opName = opConfig.getOpName();
        State currentState = trace.getCurrentState();
        List<Transition> transitions = currentState.getTransitions().stream()
                .filter(trans -> trans.getName().equals(opName))
                .collect(Collectors.toList());
        //check whether operation is executable and calculate probability whether it should be executed
        if (transitions.isEmpty() || !chooseNextOperation(opConfig, trace)) {
            return trace;
        }
        List<VariableChoice> choices = opConfig.getVariableChoices();
        Trace newTrace = trace;
        if(choices == null) {
            Random rand = new Random();
            Transition transition = transitions.get(rand.nextInt(transitions.size()));
            newTrace = newTrace.add(transition);
            delayRemainingTime(opConfig);
        } else {
            State finalCurrentState = newTrace.getCurrentState();
            String predicate = choices.stream()
                    .map(choice -> chooseVariableValues(finalCurrentState, choice.getChoice()))
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
