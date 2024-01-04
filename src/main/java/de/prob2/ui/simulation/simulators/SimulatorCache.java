package de.prob2.ui.simulation.simulators;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

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

	public String readValueWithCaching(State bState, Map<String, String> variables, String expression, SimulationHelperFunctions.EvaluationMode mode) {
		// Replace SimB variables (starting with $ first) before caching
		if(variables != null && !variables.isEmpty() && expression.contains("$")) {
			for(Map.Entry<String, String> entry : variables.entrySet()) {
				String key = entry.getKey();
				String val = entry.getValue();
				expression = expression.replaceAll(Pattern.quote("$") + key, val);
			}
		}
		IEvalElement formula = formulasCache.computeIfAbsent(expression, expr -> switch (mode) {
			case CLASSICAL_B ->
					// Use EXPAND instead of TRUNCATE, otherwise the evaluated formula is shortened to a specific length with ... in the end
					new ClassicalB(expr, FormulaExpand.EXPAND);
			case EVENT_B ->
					// Use EXPAND instead of TRUNCATE, otherwise the evaluated formula is shortened to a specific length with ... in the end
					new EventB(expr, FormulaExpand.EXPAND);
		});

		return bState.eval(formula).toString();
	}

	public List<Transition> readTransitionsWithCaching(State bState, Map<String, String> variables, String opName, String predicate, int maxTransitions) {
		if(transitionCache.keySet().size() > 5000) {
			transitionCache.clear();
		}

		// Do not replace these if branches by putIfAbsent invocations on the HashMap
		// It seems that Java evaluates the value first, before adding it to the HashMap under the condition that the corresponding key is not present
		// This would destroy the idea of caching, i.e., only evaluating when the key is absent.

		String stateID = variables.isEmpty() ? bState.getId() : bState.getId() + variables;
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
		formulasCache.clear();
		transitionCache.clear();
	}

}
