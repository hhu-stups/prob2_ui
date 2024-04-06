package de.prob2.ui.simulation.simulators;


import de.prob.formula.PredicateBuilder;
import de.prob.statespace.State;
import de.prob.statespace.Transition;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.simulation.SimulationHelperFunctions;
import de.prob2.ui.simulation.configuration.ActivationChoiceConfiguration;
import de.prob2.ui.simulation.configuration.DiagramConfiguration;
import de.prob2.ui.simulation.configuration.ActivationOperationConfiguration;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;


public class SimulationEventHandler {

	///private Random random = new Random(System.nanoTime());

	//Set fixed seed to reproduce results in paper
	private final Random random = new Random(1000);

	private final SimulatorCache cache;

	private final Simulator simulator;

	private final CurrentTrace currentTrace;


	public SimulationEventHandler(final Simulator simulator, final CurrentTrace currentTrace) {
		this.simulator = simulator;
		this.cache = new SimulatorCache();
		this.currentTrace = currentTrace;
		currentTrace.stateSpaceProperty().addListener((observable, from, to) -> cache.clear());
	}

	public String chooseVariableValues(State currentState, Map<String, String> values) {
		if(values == null) {
			return "1=1";
		}
		PredicateBuilder predicateBuilder = new PredicateBuilder();
		SimulationHelperFunctions.EvaluationMode mode = SimulationHelperFunctions.extractMode(currentTrace.getModel());
		for(String key : values.keySet()) {
			String value = values.get(key);
			String evalResult  = cache.readValueWithCaching(currentState, simulator.getVariables(), value, mode);
			predicateBuilder.add(key, evalResult);
		}
		return predicateBuilder.toString();
	}

	public Map<String, String> chooseParameters(Activation activation, State currentState) {
		Map<String, String> parameters = activation.getFixedVariables();
		if(parameters == null) {
			return null;
		}
		Map<String, String> values = new HashMap<>();
		SimulationHelperFunctions.EvaluationMode mode = SimulationHelperFunctions.extractMode(currentTrace.getModel());
		for(String parameter : parameters.keySet()) {
			String value = evaluateWithParameters(currentState, parameters.get(parameter), activation.getFiringTransitionParameters(), activation.getFiringTransitionParametersPredicate(), mode);
			values.put(parameter, value);
		}
		return values;
	}

	public Map<String, String> chooseProbabilistic(Activation activation, State currentState) {
		Object probability = activation.getProbabilisticVariables();
		if(probability == null || probability instanceof String) {
			return null;
		}
		return buildProbabilisticChoice(currentState, probability);
	}

	@SuppressWarnings("unchecked")
	private Map<String, String> buildProbabilisticChoice(State currentState, Object probability) {
		Map<String, Map<String, String>> probabilityMap = (Map<String, Map<String, String>>) probability;
		Map<String, String> values = new HashMap<>();
		for(String variable : probabilityMap.keySet()) {
			double probabilityMinimum = 0.0;
			Map<String, String> probabilityValueMap = probabilityMap.get(variable);
			double randomDouble = random.nextDouble();
			SimulationHelperFunctions.EvaluationMode mode = SimulationHelperFunctions.extractMode(currentTrace.getModel());
			for(String value : probabilityValueMap.keySet()) {
				String valueProbability = probabilityValueMap.get(value);
				double evalProbability = Double.parseDouble(cache.readValueWithCaching(currentState, simulator.getVariables(), valueProbability, SimulationHelperFunctions.EvaluationMode.CLASSICAL_B));
				if(randomDouble > probabilityMinimum && randomDouble < probabilityMinimum + evalProbability) {
					String evalValue = cache.readValueWithCaching(currentState, simulator.getVariables(), value, mode);
					values.put(variable, evalValue);
				}
				probabilityMinimum += evalProbability;
			}
			if(Math.abs(1.0 - probabilityMinimum) > 0.000001) {
				throw new RuntimeException("Sum of probabilistic choice is not equal 1");
			}
		}
		return values;
	}

	public String evaluateWithParameters(State state, String expression, List<String> parametersAsString, String parameterPredicate, SimulationHelperFunctions.EvaluationMode mode) {
		String newExpression;
		if("1=1".equals(parameterPredicate) || parametersAsString.isEmpty()) {
			newExpression = expression;
		} else {
			switch (mode) {
				case CLASSICAL_B:
					// TODO: Rises problem when one of the parameters are the empty set. In this case, the type cannot be infered. Fix this in the future by inspecting the AST.
					if(!parametersAsString.stream().map(expression::contains).findAny().get()) {
						newExpression = expression;
					} else {
						newExpression = String.format(Locale.ROOT, "LET %s BE %s IN %s END", String.join(", ", parametersAsString), parameterPredicate, expression);
					}
					break;
				case EVENT_B:
					// TODO: Rises problem when one of the parameters are the empty set. In this case, the type cannot be infered. Fix this in the future by inspecting the AST.
					if(!parametersAsString.stream().map(expression::contains).findAny().get()) {
						newExpression = expression;
					} else {
						newExpression = String.format(Locale.ROOT, "{x |-> y | x = TRUE & y : ran((%%%s.%s | %s))}(TRUE)", String.join(" |-> ", parametersAsString), parameterPredicate, expression);
					}
					break;
				default:
					throw new RuntimeException("Evaluation mode is not supported.");
			}

		}
		return cache.readValueWithCaching(state, simulator.getVariables(), newExpression, mode);
	}

	private String buildPredicateForTransition(State state, Activation activation) {
		SimulationHelperFunctions.EvaluationMode mode = SimulationHelperFunctions.extractMode(currentTrace.getModel());
		String additionalGuardsResult = activation.getAdditionalGuards() == null ? "TRUE" : cache.readValueWithCaching(state, simulator.getVariables(), activation.getAdditionalGuards(), mode);
		if("FALSE".equals(additionalGuardsResult)) {
			return "1=2";
		}

		Map<String, String> values = SimulationHelperFunctions.mergeValues(chooseProbabilistic(activation, state), chooseParameters(activation, state));
		return chooseVariableValues(state, values) + (activation.getWithPredicate() != null && !activation.getWithPredicate().isEmpty() ? " & " + activation.getWithPredicate() : "");
	}


	public Transition selectTransition(Activation activation, State currentState, Map<String, String> variables) {
		String opName = activation.getOperation();
		Object probabilisticVariables = activation.getProbabilisticVariables();
		String predicate = buildPredicateForTransition(currentState, activation);
		if(probabilisticVariables == null) {
			List<Transition> transitions = cache.readTransitionsWithCaching(currentState, variables, opName, predicate, 1);
			if (!transitions.isEmpty()) {
				return transitions.get(0);
			}
		} else if(probabilisticVariables instanceof HashMap) {
			List<Transition> transitions = cache.readTransitionsWithCaching(currentState, variables, opName, predicate, currentState.isInitialised() ? simulator.getMaxTransitions() : simulator.getMaxTransitionsBeforeInitialisation());
			if (!transitions.isEmpty()) {
				return transitions.get(0);
			}
		} else if (probabilisticVariables instanceof String probabilisticVariablesAsString){
			if("first".equals(probabilisticVariablesAsString)) {
				List<Transition> transitions = cache.readTransitionsWithCaching(currentState, variables, opName, predicate, 1);
				if (!transitions.isEmpty()) {
					return transitions.get(0);
				}
			} else if("uniform".equals(probabilisticVariablesAsString)) {
				List<Transition> transitions = cache.readTransitionsWithCaching(currentState, variables, opName, predicate, currentState.isInitialised() ? simulator.getMaxTransitions() : simulator.getMaxTransitionsBeforeInitialisation());
				if (!transitions.isEmpty()) {
					return transitions.get(random.nextInt(transitions.size()));
				}
			} else {
				throw new RuntimeException("Configuration for probabilistic choice of parameters and non-deterministic variables not supported yet");
			}
		}
		return null;
	}

	private void activateMultiOperations(List<Activation> activationsForOperation, Activation activation) {
		int insertionIndex = 0;
		while(insertionIndex < activationsForOperation.size() &&
				activation.getTime() >= activationsForOperation.get(insertionIndex).getTime()) {
			insertionIndex++;
		}
		activationsForOperation.add(insertionIndex, activation);
	}

	private void activateSingleOperations(String id, ActivationOperationConfiguration.ActivationKind activationKind, Activation activation) {
		int evaluatedTime = activation.getTime();

		List<Activation> activationsForId = simulator.getConfigurationToActivation().get(id);
		if(!activationsForId.isEmpty()) {
			switch(activationKind) {
				case SINGLE_MIN: {
					Activation activationForId = activationsForId.get(0);
					int otherActivationTime = activationForId.getTime();
					if (evaluatedTime < otherActivationTime) {
						activationsForId.clear();
						simulator.getConfigurationToActivation().get(id).add(activation);
					}
					return;
				}
				case SINGLE_MAX: {
					Activation activationForId = activationsForId.get(0);
					int otherActivationTime = activationForId.getTime();
					if (evaluatedTime > otherActivationTime) {
						activationsForId.clear();
						simulator.getConfigurationToActivation().get(id).add(activation);
					}
					return;
				}
				case SINGLE:
					return;
				default:
					break;
			}
		}

		simulator.getConfigurationToActivation().get(id).add(activation);
	}


	public void activateOperations(State state, List<String> activation, List<String> parametersAsString, String parameterPredicates) {
		if(activation != null) {
			activation.forEach(activationConfiguration -> handleOperationConfiguration(state, simulator.getActivationConfigurationMap().get(activationConfiguration), parametersAsString, parameterPredicates));
		}
	}

	public void handleOperationConfiguration(State state, DiagramConfiguration activationConfiguration, List<String> parametersAsString, String parameterPredicates) {
		if(activationConfiguration instanceof ActivationChoiceConfiguration) {
			chooseOperation(state, (ActivationChoiceConfiguration) activationConfiguration, parametersAsString, parameterPredicates);
		} else if(activationConfiguration instanceof ActivationOperationConfiguration) {
			activateOperation(state, (ActivationOperationConfiguration) activationConfiguration, parametersAsString, parameterPredicates);
		}
	}

	private void chooseOperation(State state, ActivationChoiceConfiguration activationChoiceConfiguration,
								 List<String> parametersAsString, String parameterPredicates) {
		double probabilityMinimum = 0.0;
		double randomDouble = random.nextDouble();
		for(String id : activationChoiceConfiguration.getActivations().keySet()) {
			DiagramConfiguration activationConfiguration = simulator.getActivationConfigurationMap().get(id);
			double evalProbability = Double.parseDouble(cache.readValueWithCaching(state, simulator.getVariables(), activationChoiceConfiguration.getActivations().get(id), SimulationHelperFunctions.EvaluationMode.CLASSICAL_B));
			if(randomDouble > probabilityMinimum && randomDouble < probabilityMinimum + evalProbability) {
				handleOperationConfiguration(state, activationConfiguration, parametersAsString, parameterPredicates);
			}
			probabilityMinimum += evalProbability;
		}
		if(Math.abs(1.0 - probabilityMinimum) > 0.000001) {
			throw new RuntimeException("Sum of probabilistic choice is not equal 1");
		}
	}

	public void activateOperation(State state, ActivationOperationConfiguration activationOperationConfiguration,
								   List<String> parametersAsString, String parameterPredicates) {
		List<Activation> activationsForId = simulator.getConfigurationToActivation().get(activationOperationConfiguration.getId());
		if(activationsForId == null) {
			return;
		}
		String id = activationOperationConfiguration.getId();
		String opName = activationOperationConfiguration.getOpName();
		String time = activationOperationConfiguration.getAfter();
		ActivationOperationConfiguration.ActivationKind activationKind = activationOperationConfiguration.getActivationKind();
		String additionalGuards = activationOperationConfiguration.getAdditionalGuards();
		Map<String, String> parameters = activationOperationConfiguration.getFixedVariables();
		Object probability = activationOperationConfiguration.getProbabilisticVariables();
		int evaluatedTime = Integer.parseInt(evaluateWithParameters(state, time, parametersAsString, parameterPredicates, SimulationHelperFunctions.EvaluationMode.CLASSICAL_B));
		String withPredicate = activationOperationConfiguration.getWithPredicate();

		switch (activationKind) {
			case MULTI:
				activateMultiOperations(activationsForId, new Activation(opName, evaluatedTime, additionalGuards, activationKind, parameters, probability, parametersAsString, parameterPredicates, withPredicate));
				break;
			case SINGLE:
			case SINGLE_MAX:
			case SINGLE_MIN:
				activateSingleOperations(id, activationKind, new Activation(opName, evaluatedTime, additionalGuards, activationKind, parameters, probability, parametersAsString, parameterPredicates, withPredicate));
				break;
		}
	}

	public void updateVariables(State state, Map<String, String> variables, Map<String, String> updating) {
		if(updating == null) {
			return;
		}
		SimulationHelperFunctions.EvaluationMode mode = SimulationHelperFunctions.extractMode(currentTrace.getModel());
		for(Map.Entry<String, String> entry : updating.entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue();
			String newValue = cache.readValueWithCaching(state, variables, value, mode);
			variables.put(key, newValue);
		}
	}

	public SimulatorCache getCache() {
		return cache;
	}
}
