package de.prob2.ui.simulation.simulators;

import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob.statespace.State;
import de.prob.statespace.Transition;
import de.prob2.ui.simulation.SimulationHelperFunctions;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SimulatorCache {

    private static final int MAXIMUM_CACHE_SIZE = 5000;

    private final Map<String, Map<String, String>> valuesCache = new HashMap<>();

    private final Map<String, Map<String, Map<String, List<Transition>>>> transitionCache = new HashMap<>();

    private final Map<String, Set<String>> enabledOperationsCache = new HashMap<>();

    public String readValueWithCaching(State bState, String expression) {
        if(valuesCache.keySet().size() > MAXIMUM_CACHE_SIZE) {
            valuesCache.clear();
        }
        String stateID = bState.getId();
        String value;

        // Do not replace these if branches by putIfAbsent invocations on the HashMap
        // It seems that Java evaluates the value first, before adding it to the HashMap under the condition that the corresponding key is not present
        // This would destroy the idea of caching, i.e., only evaluating when the key is absent.

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
        if(transitionCache.keySet().size() > 5000) {
            transitionCache.clear();
        }

        // Do not replace these if branches by putIfAbsent invocations on the HashMap
        // It seems that Java evaluates the value first, before adding it to the HashMap under the condition that the corresponding key is not present
        // This would destroy the idea of caching, i.e., only evaluating when the key is absent.

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

    public Set<String> readEnabledOperationsWithCaching(State bState) {
        if(enabledOperationsCache.size() > 5000) {
            enabledOperationsCache.clear();
        }
        String stateID = bState.getId();

        // Do not replace these if branches by putIfAbsent invocations on the HashMap
        // It seems that Java evaluates the value first, before adding it to the HashMap under the condition that the corresponding key is not present
        // This would destroy the idea of caching, i.e., only evaluating when the key is absent.

        if(!enabledOperationsCache.containsKey(stateID)) {
            Set<String> operations = bState.getOutTransitions().stream()
                    .map(Transition::getName)
                    .collect(Collectors.toSet());
            enabledOperationsCache.put(stateID, operations);
        }
        return enabledOperationsCache.get(stateID);
    }

    public void clear() {
        valuesCache.clear();
        transitionCache.clear();
        enabledOperationsCache.clear();
    }

}
