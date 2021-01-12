package de.prob2.ui.simulation.simulators;

import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob.statespace.State;
import de.prob.statespace.Transition;
import de.prob2.ui.simulation.SimulationHelperFunctions;
import de.prob2.ui.simulation.configuration.OperationConfiguration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SimulatorCache {

    private final Map<String, Map<String, Double>> probabilityCache = new HashMap<>();

    private final Map<String, Map<String, String>> valuesCache = new HashMap<>();

    private final Map<String, Map<String, List<Transition>>> transitionCache = new HashMap<>();

    public double readProbabilityWithCaching(State bState, String opName, String probability) {
        String stateID = bState.getId();

        double opProbability;
        if(probabilityCache.containsKey(stateID)) {
            if(probabilityCache.get(stateID).containsKey(opName)) {
                opProbability = probabilityCache.get(stateID).get(opName);
            } else {
                AbstractEvalResult evalResult = SimulationHelperFunctions.evaluateForSimulation(bState, probability);
                opProbability = Double.parseDouble(evalResult.toString());
                probabilityCache.get(stateID).put(opName, opProbability);
            }
        } else {
            probabilityCache.put(stateID, new HashMap<>());
            AbstractEvalResult evalResult = SimulationHelperFunctions.evaluateForSimulation(bState, probability);
            opProbability = Double.parseDouble(evalResult.toString());
            probabilityCache.get(stateID).put(opName, opProbability);
        }
        return opProbability;
    }

    public String readValueWithCaching(State bState, String expression) {
        String stateID = bState.getId();
        String value;
        if(valuesCache.containsKey(stateID)) {
            if(valuesCache.get(stateID).containsKey(expression)) {
                value = valuesCache.get(stateID).get(expression);
            } else {
                AbstractEvalResult evalResult = SimulationHelperFunctions.evaluateForSimulation(bState, expression);
                value = evalResult.toString();
                valuesCache.get(stateID).put(expression, value);
            }
        } else {
            valuesCache.put(stateID, new HashMap<>());
            AbstractEvalResult evalResult = SimulationHelperFunctions.evaluateForSimulation(bState, expression);
            value = evalResult.toString();
            valuesCache.get(stateID).put(expression, value);
        }
        return value;
    }

    private void loadTransitionsInCache(State currentState, String opName) {
        String stateID = currentState.getId();
        if(!transitionCache.containsKey(stateID)) {
            transitionCache.put(stateID, new HashMap<>());
        }
        transitionCache.get(stateID).put(opName, currentState.getOutTransitions().stream()
                .filter(trans -> trans.getName().equals(opName))
                .collect(Collectors.toList()));
    }

    public List<Transition> readTransitionsWithCaching(State bState, String opName) {
        String stateID = bState.getId();

        if(!transitionCache.containsKey(stateID) || !transitionCache.get(stateID).containsKey(opName)) {
            loadTransitionsInCache(bState, opName);
        }

        return transitionCache.get(bState.getId()).get(opName);
    }

	public Map<String, Map<String, List<Transition>>> getTransitionCache() {
		return transitionCache;
	}
}
