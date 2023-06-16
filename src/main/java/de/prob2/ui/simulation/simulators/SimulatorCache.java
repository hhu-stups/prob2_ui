package de.prob2.ui.simulation.simulators;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import de.prob.animator.domainobjects.ClassicalB;
import de.prob.animator.domainobjects.EventB;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.statespace.State;
import de.prob.statespace.Transition;
import de.prob2.ui.simulation.SimulationHelperFunctions;

public class SimulatorCache {
	private final Map<String, IEvalElement> formulasCache = new HashMap<>();

	private final Map<String, Map<String, Map<String, List<Transition>>>> transitionCache = new HashMap<>();

	private final Map<String, Set<String>> enabledOperationsCache = new HashMap<>();

	public String readValueWithCaching(State bState, Map<String, String> variables, String expression, SimulationHelperFunctions.EvaluationMode mode) {
		IEvalElement formula = formulasCache.computeIfAbsent(expression, expr -> {
			if(variables != null && !variables.isEmpty() && expr.contains("$")) {
				for(Map.Entry<String, String> entry : variables.entrySet()) {
					String key = entry.getKey();
					String val = entry.getValue();
					expr = expr.replaceAll(Pattern.quote("$") + key, val);
				}
			}

			switch (mode) {
				case CLASSICAL_B:
					// Use EXPAND instead of TRUNCATE, otherwise the evaluated formula is shortened to a specific length with ... in the end
					return new ClassicalB(expr, FormulaExpand.EXPAND);
				case EVENT_B:
					// Use EXPAND instead of TRUNCATE, otherwise the evaluated formula is shortened to a specific length with ... in the end
					return new EventB(expr, FormulaExpand.EXPAND);
				default:
					throw new RuntimeException("Evaluation mode is not supported");
			}
		});

		return bState.eval(formula).toString();
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
		formulasCache.clear();
		transitionCache.clear();
		enabledOperationsCache.clear();
	}

}
