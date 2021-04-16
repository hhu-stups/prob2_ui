package de.prob2.ui.simulation.simulators;


import de.prob.formula.PredicateBuilder;
import de.prob.statespace.State;
import de.prob.statespace.Transition;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.simulation.SimulationHelperFunctions;
import de.prob2.ui.simulation.configuration.ActivationChoiceConfiguration;
import de.prob2.ui.simulation.configuration.ActivationConfiguration;
import de.prob2.ui.simulation.configuration.ActivationOperationConfiguration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;


public class SimulationEventHandler {

	///private Random random = new Random(System.nanoTime());

	//Set fixed seed to reproduce results in paper
	private final Random random = new Random(1000);

	private final SimulatorCache cache;

	private final Simulator simulator;


	public SimulationEventHandler(final Simulator simulator, final CurrentTrace currentTrace) {
		this.simulator = simulator;
		this.cache = new SimulatorCache();
		currentTrace.stateSpaceProperty().addListener((observable, from, to) -> cache.clear());
	}

	public String chooseVariableValues(State currentState, Map<String, String> values) {
		if(values == null) {
			return "1=1";
		}
		PredicateBuilder predicateBuilder = new PredicateBuilder();
		for(String key : values.keySet()) {
			String value = values.get(key);
			String evalResult  = cache.readValueWithCaching(currentState, value);
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
		for(String parameter : parameters.keySet()) {
			String value = evaluateWithParameters(currentState, parameters.get(parameter), activation.getFiringTransitionParameters(), activation.getFiringTransitionParametersPredicate());
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
			for(String value : probabilityValueMap.keySet()) {
				String valueProbability = probabilityValueMap.get(value);
				double evalProbability = Double.parseDouble(cache.readValueWithCaching(currentState, valueProbability));
				if(randomDouble > probabilityMinimum && randomDouble < probabilityMinimum + evalProbability) {
					String evalValue = cache.readValueWithCaching(currentState, value);
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

	private String evaluateWithParameters(State state, String expression, List<String> parametersAsString, String parameterPredicate) {
		String newExpression;
		if("1=1".equals(parameterPredicate) || parametersAsString.isEmpty()) {
			newExpression = expression;
		} else {
			newExpression = String.format("LET %s BE %s IN %s END", String.join(", ", parametersAsString), parameterPredicate, expression);
		}
		return cache.readValueWithCaching(state, newExpression);
	}

	private String buildPredicateForTransition(State state, Activation activation) {
		String additionalGuardsResult = activation.getAdditionalGuards() == null ? "TRUE" : cache.readValueWithCaching(state, activation.getAdditionalGuards());
		if("FALSE".equals(additionalGuardsResult)) {
			return "1=2";
		}

		Map<String, String> values = SimulationHelperFunctions.mergeValues(chooseProbabilistic(activation, state), chooseParameters(activation, state));
		return chooseVariableValues(state, values);
	}


	public Transition selectTransition(Activation activation, State currentState) {
		String opName = activation.getOperation();
		Object probabilisticVariables = activation.getProbabilisticVariables();
		String predicate = buildPredicateForTransition(currentState, activation);
		if(probabilisticVariables == null) {
			List<Transition> transitions = cache.readTransitionsWithCaching(currentState, opName, predicate, 1);
			if(transitions.size() > 0) {
				return transitions.get(0);
			}
		} else if(probabilisticVariables instanceof HashMap) {
			List<Transition> transitions = cache.readTransitionsWithCaching(currentState, opName, predicate, currentState.isInitialised() ? simulator.getMaxTransitions() : simulator.getMaxTransitionsBeforeInitialisation());
			if(transitions.size() >= 1) {
				return transitions.get(0);
			}
		} else if (probabilisticVariables instanceof String){
			String probabilisticVariablesAsString = (String) probabilisticVariables;
			if("first".equals(probabilisticVariablesAsString)) {
				List<Transition> transitions = cache.readTransitionsWithCaching(currentState, opName, predicate, 1);
				if(transitions.size() > 0) {
					return transitions.get(0);
				}
			} else if("uniform".equals(probabilisticVariablesAsString)) {
				List<Transition> transitions = cache.readTransitionsWithCaching(currentState, opName, predicate, currentState.isInitialised() ? simulator.getMaxTransitions() : simulator.getMaxTransitionsBeforeInitialisation());
				if(transitions.size() > 0) {
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

	private void activateSingleOperations(String id, String opName, ActivationOperationConfiguration.ActivationKind activationKind, Activation activation) {
		Set<String> activationsForOperation = simulator.getOperationToActivations().get(opName);
		int evaluatedTime = activation.getTime();

		for(String activationId : activationsForOperation) {
			List<Activation> activationsForId = simulator.getConfigurationToActivation().get(activationId);
			if(activationsForId.isEmpty()) {
				continue;
			}
			switch(activationKind) {
				case SINGLE_MIN: {
					Activation activationForId = activationsForId.get(0);
					int otherActivationTime = activationForId.getTime();
					if (evaluatedTime < otherActivationTime) {
						activationsForId.clear();
						simulator.getConfigurationToActivation().get(id).add(activation);
						return;
					}
					break;
				}
				case SINGLE_MAX: {
					Activation activationForId = activationsForId.get(0);
					int otherActivationTime = activationForId.getTime();
					if (evaluatedTime > otherActivationTime) {
						activationsForId.clear();
						simulator.getConfigurationToActivation().get(id).add(activation);
						return;
					}
					break;
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

	private void handleOperationConfiguration(State state, ActivationConfiguration activationConfiguration, List<String> parametersAsString, String parameterPredicates) {
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
			ActivationConfiguration activationConfiguration = simulator.getActivationConfigurationMap().get(id);
			double evalProbability = Double.parseDouble(cache.readValueWithCaching(state, activationChoiceConfiguration.getActivations().get(id)));
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
		List<Activation> activationsForOperation = simulator.getConfigurationToActivation().get(activationOperationConfiguration.getId());
		if(activationsForOperation == null) {
			return;
		}
		String id = activationOperationConfiguration.getId();
		String opName = activationOperationConfiguration.getOpName();
		String time = activationOperationConfiguration.getAfter();
		ActivationOperationConfiguration.ActivationKind activationKind = activationOperationConfiguration.getActivationKind();
		String additionalGuards = activationOperationConfiguration.getAdditionalGuards();
		Map<String, String> parameters = activationOperationConfiguration.getFixedVariables();
		Object probability = activationOperationConfiguration.getProbabilisticVariables();
		int evaluatedTime = Integer.parseInt(evaluateWithParameters(state, time, parametersAsString, parameterPredicates));

		switch (activationKind) {
			case MULTI:
				activateMultiOperations(activationsForOperation, new Activation(opName, evaluatedTime, additionalGuards, activationKind, parameters, probability, parametersAsString, parameterPredicates));
				break;
			case SINGLE:
			case SINGLE_MAX:
			case SINGLE_MIN:
				activateSingleOperations(id, opName, activationKind, new Activation(opName, evaluatedTime, additionalGuards, activationKind, parameters, probability, parametersAsString, parameterPredicates));
				break;
		}
	}

	public SimulatorCache getCache() {
		return cache;
	}
}
