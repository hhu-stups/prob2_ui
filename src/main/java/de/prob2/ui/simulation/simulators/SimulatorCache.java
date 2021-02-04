package de.prob2.ui.simulation.simulators;

import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob.statespace.State;
import de.prob.statespace.Transition;
import de.prob2.ui.simulation.SimulationHelperFunctions;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SimulatorCache {

    private final Map<String, Map<String, String>> valuesCache = new HashMap<>();

    private final Map<String, Map<String, Map<String, List<Transition>>>> transitionCache = new HashMap<>();

    public String readValueWithCaching(State bState, String expression) {
        String stateID = bState.getId();
        String value;

        if(!valuesCache.containsKey(stateID) || !valuesCache.get(stateID).containsKey(expression)) {
            // loads values in cache if necessary
            if(!valuesCache.containsKey(stateID)) {
                valuesCache.put(stateID, new HashMap<>());
            }
            AbstractEvalResult evalResult = SimulationHelperFunctions.evaluateForSimulation(bState, expression);
            value = evalResult.toString();
            valuesCache.get(stateID).put(expression, value);
        }
        // finally get value from cache
        return valuesCache.get(stateID).get(expression);
    }

    public List<Transition> readTransitionsWithCaching(State bState, String opName, String predicate, int maxTransitions) {
        String stateID = bState.getId();
        if(!transitionCache.containsKey(stateID) || !transitionCache.get(stateID).containsKey(opName) ||
				!transitionCache.get(stateID).get(opName).containsKey(predicate)) {
            // loads transitions in cache if necessary
            if(!transitionCache.containsKey(stateID)) {
                transitionCache.put(stateID, new HashMap<>());
            }
            if(!transitionCache.get(stateID).containsKey(opName)) {
            	transitionCache.get(stateID).put(opName, new HashMap<>());
			}
            transitionCache.get(stateID).get(opName).put(predicate, bState.findTransitions(opName, Collections.singletonList(predicate), maxTransitions));
        }
        return transitionCache.get(stateID).get(opName).get(predicate);
    }

    public void clear() {
        valuesCache.clear();
        transitionCache.clear();
    }

}
