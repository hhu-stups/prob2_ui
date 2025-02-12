package de.prob2.ui.simulation.simulators;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import de.prob.formula.PredicateBuilder;
import de.prob.statespace.State;
import de.prob.statespace.Transition;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.simulation.EvaluationMode;
import de.prob2.ui.simulation.configuration.ActivationChoiceConfiguration;
import de.prob2.ui.simulation.configuration.ActivationKind;
import de.prob2.ui.simulation.configuration.ActivationOperationConfiguration;
import de.prob2.ui.simulation.configuration.DiagramConfiguration;

public class SimulationEventHandler {

	///private Random random = new Random(System.nanoTime());

	//Set fixed seed to reproduce results in paper
	private final Random random = new Random(1000);

	private final SimulatorCache cache;

	private final Simulator simulator;

	private final CurrentTrace currentTrace;

	private final CurrentProject currentProject;

	private final LinkedList<String> visitedChoiceIDs;


	public SimulationEventHandler(final Simulator simulator, final CurrentTrace currentTrace, final CurrentProject currentProject) {
		this.simulator = simulator;
		this.cache = new SimulatorCache();
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.visitedChoiceIDs = new LinkedList<>();
		currentTrace.stateSpaceProperty().addListener((observable, from, to) -> cache.clear());

		this.currentProject.addListener((observable, from, to) -> {
			if((from == null && to != null) || !Objects.equals(from, to)) {
				cache.clear();
			}
		});

		this.currentProject.currentMachineProperty().addListener((observable, from, to) -> {
			if((from == null && to != null) || !Objects.equals(from, to)) {
				cache.clear();
			}
		});
	}

	public String chooseVariableValues(State currentState, Map<String, String> values) {
		if(values == null) {
			return "1=1";
		}
		PredicateBuilder predicateBuilder = new PredicateBuilder();
		EvaluationMode mode = EvaluationMode.extractMode(currentTrace.getModel());
		for(String key : values.keySet()) {
			String value = values.get(key);
			String evalResult  = cache.readValueWithCaching(currentState, simulator.getVariables(), value, mode);
			predicateBuilder.add(key, evalResult);
		}
		return predicateBuilder.toString();
	}

	public Map<String, String> chooseParameters(Activation activation, State currentState) {
		var parameters = activation.fixedVariables();
		Map<String, String> values = new HashMap<>();
		if (parameters != null) {
			EvaluationMode mode = EvaluationMode.extractMode(currentTrace.getModel());
			for (var e : parameters.entrySet()) {
				String value = evaluateWithParameters(currentState, e.getValue(), activation.firingTransitionParameters(), activation.firingTransitionParametersPredicate(), mode);
				values.put(e.getKey(), value);
			}
		}
		return values;
	}

	public Map<String, String> chooseProbabilistic(Activation activation, State currentState) {
		var probabilities = activation.probabilisticVariables();
		EvaluationMode mode = EvaluationMode.extractMode(currentTrace.getModel());
		Map<String, String> values = new HashMap<>();
		// TODO: use weights instead of forcing the probabilities to sum to 1
		for (var e1 : probabilities.entrySet()) {
			var variable = e1.getKey();
			var probabilityValueMap = e1.getValue();
			var probabilityMinimum = 0.0;
			var randomDouble = random.nextDouble();
			for (var e2 : probabilityValueMap.entrySet()) {
				var value = e2.getKey();
				var valueProbability = e2.getValue();
				var evalProbability = Double.parseDouble(cache.readValueWithCaching(currentState, simulator.getVariables(), valueProbability, EvaluationMode.CLASSICAL_B));
				if (randomDouble > probabilityMinimum && randomDouble < probabilityMinimum + evalProbability) {
					String evalValue = cache.readValueWithCaching(currentState, simulator.getVariables(), value, mode);
					values.put(variable, evalValue);
				}
				probabilityMinimum += evalProbability;
			}
			if (Math.abs(1.0 - probabilityMinimum) > 0.000001) {
				throw new RuntimeException("Sum of probabilistic choice is not equal 1");
			}
		}
		return values;
	}

	public String evaluateWithParameters(State state, String expression, List<String> parametersAsString, String parameterPredicate, EvaluationMode mode) {
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
		EvaluationMode mode = EvaluationMode.extractMode(currentTrace.getModel());
		String additionalGuardsResult = activation.additionalGuards().isEmpty() || "1=1".equals(activation.additionalGuards()) ? "TRUE" : cache.readValueWithCaching(state, simulator.getVariables(), activation.additionalGuards(), mode);
		if ("FALSE".equals(additionalGuardsResult)) {
			return "1=2";
		}

		Map<String, String> values = new HashMap<>(chooseProbabilistic(activation, state));
		values.putAll(chooseParameters(activation, state));

		return chooseVariableValues(state, values) + (activation.withPredicate() != null && !activation.withPredicate().isEmpty() ? " & " + activation.withPredicate() : "");
	}

	public Transition selectTransition(Activation activation, State currentState, Map<String, String> variables) {
		String opName = activation.operation();
		String predicate = buildPredicateForTransition(currentState, activation);
		return switch (activation.transitionSelection()) {
			case FIRST -> {
				List<Transition> transitions = cache.readTransitionsWithCaching(currentState, variables, opName, predicate, 1);
				if (!transitions.isEmpty()) {
					yield transitions.get(0);
				} else {
					yield null;
				}
			}
			case UNIFORM -> {
				List<Transition> transitions = cache.readTransitionsWithCaching(currentState, variables, opName, predicate, currentState.isInitialised() ? simulator.getMaxTransitions() : simulator.getMaxTransitionsBeforeInitialisation());
				int len = transitions.size();
				if (len == 1) {
					yield transitions.get(0);
				} else if (len > 1) {
					yield transitions.get(random.nextInt(len));
				} else {
					yield null;
				}
			}
		};
	}

	private void activateMultiOperations(List<Activation> activationsForOperation, Activation activation) {
		int insertionIndex = 0;
		while (insertionIndex < activationsForOperation.size()
				&& activation.time() >= activationsForOperation.get(insertionIndex).time()) {
			insertionIndex++;
		}
		activationsForOperation.add(insertionIndex, activation);
	}

	private void activateSingleOperations(String id, ActivationKind activationKind, Activation activation) {
		int evaluatedTime = activation.time();

		List<Activation> activationsForId = simulator.getConfigurationToActivation().get(id);
		if(!activationsForId.isEmpty()) {
			switch(activationKind) {
				case SINGLE_MIN: {
					Activation activationForId = activationsForId.get(0);
					int otherActivationTime = activationForId.time();
					if (evaluatedTime < otherActivationTime) {
						activationsForId.clear();
						simulator.getConfigurationToActivation().get(id).add(activation);
					}
					return;
				}
				case SINGLE_MAX: {
					Activation activationForId = activationsForId.get(0);
					int otherActivationTime = activationForId.time();
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
			if(visitedChoiceIDs.contains(activationConfiguration.getId())) {
				throw new RuntimeException("Cycle in activation diagram detected");
			}
			visitedChoiceIDs.push(activationConfiguration.getId());
			chooseOperation(state, (ActivationChoiceConfiguration) activationConfiguration, parametersAsString, parameterPredicates);
			visitedChoiceIDs.pop();
		} else if(activationConfiguration instanceof ActivationOperationConfiguration) {
			activateOperation(state, (ActivationOperationConfiguration) activationConfiguration, parametersAsString, parameterPredicates);
		}
	}

	private void chooseOperation(State state, ActivationChoiceConfiguration activationChoiceConfiguration,
								 List<String> parametersAsString, String parameterPredicates) {
		// TODO: use weights instead of forcing the probabilities to sum to 1
		double probabilityMinimum = 0.0;
		double randomDouble = random.nextDouble();
		for(String id : activationChoiceConfiguration.getChooseActivation().keySet()) {
			DiagramConfiguration activationConfiguration = simulator.getActivationConfigurationMap().get(id);
			double evalProbability = Double.parseDouble(cache.readValueWithCaching(state, simulator.getVariables(), activationChoiceConfiguration.getChooseActivation().get(id), EvaluationMode.CLASSICAL_B));
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
		String opName = activationOperationConfiguration.getExecute();
		String time = activationOperationConfiguration.getAfter();
		ActivationKind activationKind = activationOperationConfiguration.getActivationKind();
		String additionalGuards = activationOperationConfiguration.getAdditionalGuards();
		Map<String, String> parameters = activationOperationConfiguration.getFixedVariables();
		var probabilities = activationOperationConfiguration.getProbabilisticVariables();
		var transitionSelection = activationOperationConfiguration.getTransitionSelection();
		int evaluatedTime = Integer.parseInt(evaluateWithParameters(state, time, parametersAsString, parameterPredicates, EvaluationMode.CLASSICAL_B));
		String withPredicate = activationOperationConfiguration.getWithPredicate();

		var activation = new Activation(opName, evaluatedTime, additionalGuards, activationKind, parameters, probabilities, transitionSelection, parametersAsString, parameterPredicates, withPredicate);
		switch (activationKind) {
			case MULTI:
				activateMultiOperations(activationsForId, activation);
				break;
			case SINGLE:
			case SINGLE_MAX:
			case SINGLE_MIN:
				activateSingleOperations(id, activationKind, activation);
				break;
		}
	}

	public void updateVariables(State state, Map<String, String> variables, Map<String, String> updating) {
		if(updating == null) {
			return;
		}
		EvaluationMode mode = EvaluationMode.extractMode(currentTrace.getModel());
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
