package de.prob2.ui.simulation.simulators;

import de.prob.animator.command.GetPreferenceCommand;
import de.prob.statespace.State;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.simulation.SimulationHelperFunctions;
import de.prob2.ui.simulation.configuration.ActivationChoiceConfiguration;
import de.prob2.ui.simulation.configuration.ActivationConfiguration;
import de.prob2.ui.simulation.configuration.ActivationOperationConfiguration;
import de.prob2.ui.simulation.configuration.SimulationConfiguration;
import de.prob2.ui.simulation.configuration.SimulationConfigurationChecker;
import de.prob2.ui.simulation.configuration.SimulationFileHandler;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class Simulator {

    protected Random random = new Random(System.nanoTime());

    protected SimulationConfiguration config;

    protected IntegerProperty time;

    protected int delay;

    protected int stepCounter;

    protected Map<String, List<Activation>> configurationToActivation;

    protected List<ActivationOperationConfiguration> activationConfigurationsSorted;

    protected Map<String, ActivationConfiguration> activationConfigurationMap;

    protected Map<String, Set<String>> operationToActivations;

    protected SimulatorCache cache;

    protected final CurrentTrace currentTrace;

    protected ChangeListener<? super Trace> traceListener = null;

	protected int maxTransitionsBeforeInitialisation;

	protected int maxTransitions;

	protected List<Integer> timestamps;

	public Simulator(final CurrentTrace currentTrace) {
        super();
        this.currentTrace = currentTrace;
        this.time = new SimpleIntegerProperty(0);
        this.cache = new SimulatorCache();
        this.traceListener = (observable, from, to) -> {
            if(config != null && to != null && to.getStateSpace() != null) {
                checkConfiguration(to.getStateSpace());
				GetPreferenceCommand cmd = new GetPreferenceCommand("MAX_INITIALISATIONS");
				currentTrace.getStateSpace().execute(cmd);
				this.maxTransitionsBeforeInitialisation = Integer.parseInt(cmd.getValue());

				cmd = new GetPreferenceCommand("MAX_OPERATIONS");
				currentTrace.getStateSpace().execute(cmd);
				this.maxTransitions = Integer.parseInt(cmd.getValue());
                currentTrace.removeListener(traceListener);
            }
        };
	}

    public String chooseVariableValues(State currentState, Map<String, String> values) {
	    if(values == null || values.isEmpty()) {
	        return "1=1";
        }
        StringBuilder conjuncts = new StringBuilder();
        for(Iterator<String> it = values.keySet().iterator(); it.hasNext();) {
            String key = it.next();
            String value = values.get(key);
            String evalResult  = cache.readValueWithCaching(currentState, value);
            conjuncts.append(key);
            conjuncts.append(" = ");
            conjuncts.append(evalResult);
            if(it.hasNext()) {
                conjuncts.append(" & ");
            }
        }
        return conjuncts.toString();
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
        Map<String, String> values = null;

        Object probability = activation.getProbabilisticVariables();
        if(probability == null) {
            return values;
        }
        //choose between non-deterministic assigned variables
        if(probability instanceof String) {
            if("uniform".equals(probability.toString())) {
                return null;
            }
        } else {
            Map<String, Map<String, String>> probabilityMap = (Map<String, Map<String, String>>) probability;
            values = new HashMap<>();
            for(String variable : probabilityMap.keySet()) {
                double probabilityMinimum = 0.0;
                Object probabilityValue = probabilityMap.get(variable);
                Map<String, String> probabilityValueMap = (Map<String, String>) probabilityValue;
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
            }
        }

        return values;
    }

    public void initSimulator(File configFile) throws IOException {
        this.config = SimulationFileHandler.constructConfigurationFromJSON(configFile);
        if(currentTrace.get() != null && currentTrace.getStateSpace() != null) {
            checkConfiguration(currentTrace.getStateSpace());
			GetPreferenceCommand cmd = new GetPreferenceCommand("MAX_INITIALISATIONS");
			currentTrace.getStateSpace().execute(cmd);
			this.maxTransitionsBeforeInitialisation = Integer.parseInt(cmd.getValue());

			cmd = new GetPreferenceCommand("MAX_OPERATIONS");
			currentTrace.getStateSpace().execute(cmd);
			this.maxTransitions = Integer.parseInt(cmd.getValue());
        } else {
            currentTrace.addListener(traceListener);
        }
        resetSimulator();
    }

    public void initSimulator(SimulationConfiguration config) throws IOException {
        this.config = config;
        if(currentTrace.get() != null && currentTrace.getStateSpace() != null) {
            checkConfiguration(currentTrace.getStateSpace());
            GetPreferenceCommand cmd = new GetPreferenceCommand("MAX_INITIALISATIONS");
            currentTrace.getStateSpace().execute(cmd);
            this.maxTransitionsBeforeInitialisation = Integer.parseInt(cmd.getValue());

            cmd = new GetPreferenceCommand("MAX_OPERATIONS");
            currentTrace.getStateSpace().execute(cmd);
            this.maxTransitions = Integer.parseInt(cmd.getValue());
        } else {
            currentTrace.addListener(traceListener);
        }
        resetSimulator();
    }

    protected void checkConfiguration(StateSpace stateSpace) throws RuntimeException {
        SimulationConfigurationChecker simulationConfigurationChecker = new SimulationConfigurationChecker(stateSpace, this.config);
        simulationConfigurationChecker.check();
        if(!simulationConfigurationChecker.getErrors().isEmpty()) {
            throw new RuntimeException(simulationConfigurationChecker.getErrors().stream().map(Throwable::getMessage).collect(Collectors.joining("\n")));
        }
    }

    public void resetSimulator() {
        this.configurationToActivation = new HashMap<>();
		this.activationConfigurationMap = new HashMap<>();
		this.operationToActivations = new HashMap<>();
		this.timestamps = new ArrayList<>();

        this.delay = 0;
        this.time.set(0);
        this.stepCounter = 0;
        if(config != null) {
            // sort after priority
            this.activationConfigurationsSorted = config.getActivationConfigurations().stream()
                    .filter(activationConfiguration -> activationConfiguration instanceof ActivationOperationConfiguration)
                    .map(activationConfiguration -> (ActivationOperationConfiguration) activationConfiguration)
                    .sorted(Comparator.comparingInt(ActivationOperationConfiguration::getPriority))
                    .collect(Collectors.toList());

            config.getActivationConfigurations().forEach(activationConfiguration -> activationConfigurationMap.put(activationConfiguration.getId(), activationConfiguration));
            config.getActivationConfigurations().stream()
                    .filter(activationConfiguration -> activationConfiguration instanceof ActivationOperationConfiguration)
                    .map(activationConfiguration -> (ActivationOperationConfiguration) activationConfiguration)
                    .forEach(activationConfiguration -> {
                        String opName = activationConfiguration.getOpName();
                        if(!operationToActivations.containsKey(opName)) {
                            operationToActivations.put(opName, new HashSet<>());
                        }
                        operationToActivations.get(opName).add(activationConfiguration.getId());
                    });
			activationConfigurationsSorted.forEach(config -> configurationToActivation.put(config.getId(), new ArrayList<>()));
            currentTrace.removeListener(traceListener);
        }
    }

    public void updateRemainingTime() {
        updateRemainingTime(this.delay);
    }

    public void updateRemainingTime(int delay) {
        this.time.set(this.time.get() + delay);
        for(String key : configurationToActivation.keySet()) {
            for(Activation activation : configurationToActivation.get(key)) {
                activation.decreaseTime(delay);
            }
        }
    }

    public void updateDelay() {
        int delay = Integer.MAX_VALUE;
        for(List<Activation> activations : configurationToActivation.values()) {
            for(Activation activation : activations) {
                if(activation.getTime() < delay) {
                    delay = activation.getTime();
                }
            }
        }
        this.delay = delay;
    }

    protected Trace simulationStep(Trace trace) {
        Trace newTrace = trace;
		updateRemainingTime();
		newTrace = executeActivatedOperations(newTrace);
		updateDelay();
        return newTrace;
    }

    public void setupBeforeSimulation(Trace trace) {
        State currentState = trace.getCurrentState();
        if(!currentState.isInitialised()) {
        	if(configurationToActivation.containsKey("$setup_constants")) {
				ActivationOperationConfiguration setupConfiguration = (ActivationOperationConfiguration) activationConfigurationMap.get("$setup_constants");
				activateOperation(trace.getCurrentState(), setupConfiguration, new ArrayList<>(), "1=1");
			}
			if(configurationToActivation.containsKey("$initialise_machine")) {
				ActivationOperationConfiguration initConfiguration = (ActivationOperationConfiguration) activationConfigurationMap.get("$initialise_machine");
				activateOperation(trace.getCurrentState(), initConfiguration, new ArrayList<>(), "1=1");
			}
        }
    }

    protected Trace executeActivatedOperations(Trace trace) {
        Trace newTrace = trace;
        for(ActivationOperationConfiguration opConfig : activationConfigurationsSorted) {
            if (endingConditionReached(newTrace)) {
                break;
            }
            newTrace = executeActivatedOperation(opConfig, newTrace);
        }
        return newTrace;
    }

    protected String evaluateWithParameters(State state, String expression, List<String> parametersAsString, String parameterPredicate) {
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

    public Trace executeActivatedOperation(ActivationOperationConfiguration activationConfig, Trace trace) {
        String id = activationConfig.getId();
	    String chosenOp = activationConfig.getOpName();
        List<String> activationConfiguration = activationConfig.getActivating();

        List<Activation> activationForOperation = configurationToActivation.get(id);
        List<Activation> activationForOperationCopy = new ArrayList<>(activationForOperation);

        Trace newTrace = trace;
        for(Activation activation : activationForOperationCopy) {
            //select operation only if its time is 0
            if(activation.getTime() > 0) {
                break;
            }
            activationForOperation.remove(activation);

            State currentState = newTrace.getCurrentState();
			String predicate = buildPredicateForTransition(currentState, activation);
			List<Transition> transitions = cache.readTransitionsWithCaching(currentState, chosenOp, predicate, maxTransitions);
            if (!transitions.isEmpty()) {
                Transition transition = transitions.get(random.nextInt(transitions.size()));
                newTrace = newTrace.add(transition);
                List<String> parameterNames = transition.getParameterNames() == null ? new ArrayList<>() : transition.getParameterNames();
                String parameterPredicate = transition.getParameterPredicate() == null ? "1=1" : transition.getParameterPredicate();
                activateOperations(newTrace.getCurrentState(), activationConfiguration, parameterNames, parameterPredicate);
            	timestamps.add(time.get());
            }
        }
        stepCounter = newTrace.getTransitionList().size();
        return newTrace;
    }

    protected void activateMultiOperations(List<Activation> activationsForOperation, Activation activation) {
        int insertionIndex = 0;
        while(insertionIndex < activationsForOperation.size() &&
                activation.getTime() >= activationsForOperation.get(insertionIndex).getTime()) {
            insertionIndex++;
        }
        activationsForOperation.add(insertionIndex, activation);
    }

    protected void activateSingleOperations(String id, String opName, ActivationOperationConfiguration.ActivationKind activationKind, Activation activation) {
	    Set<String> activationsForOperation = operationToActivations.get(opName);
	    int evaluatedTime = activation.getTime();

	    for(String activationId : activationsForOperation) {
            List<Activation> activationsForId = configurationToActivation.get(activationId);
            if(activationsForId.isEmpty()) {
                continue;
            }
	        switch(activationKind) {
                case SINGLE_MIN: {
                    Activation activationForId = activationsForId.get(0);
                    int otherActivationTime = activationForId.getTime();
                    if (evaluatedTime < otherActivationTime) {
                        activationsForId.clear();
                        configurationToActivation.get(id).add(activation);
                        return;
                    }
                    break;
                }
                case SINGLE_MAX: {
                    Activation activationForId = activationsForId.get(0);
                    int otherActivationTime = activationForId.getTime();
                    if (evaluatedTime > otherActivationTime) {
                        activationsForId.clear();
                        configurationToActivation.get(id).add(activation);
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
        configurationToActivation.get(id).add(activation);
    }

    public void activateOperations(State state, List<String> activation, List<String> parametersAsString, String parameterPredicates) {
        if(activation != null) {
            activation.forEach(activationConfiguration -> handleOperationConfiguration(state, activationConfigurationMap.get(activationConfiguration), parametersAsString, parameterPredicates));
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
        for(int i = 0; i < activationChoiceConfiguration.getActivations().size(); i++) {
            ActivationConfiguration activationConfiguration = activationConfigurationMap.get(activationChoiceConfiguration.getActivations().get(i));;
            double evalProbability = Double.parseDouble(cache.readValueWithCaching(state, activationChoiceConfiguration.getProbability().get(i)));
            if(randomDouble > probabilityMinimum && randomDouble < probabilityMinimum + evalProbability) {
                handleOperationConfiguration(state, activationConfiguration, parametersAsString, parameterPredicates);
                break;
            }
            probabilityMinimum += evalProbability;
        }
    }

    private void activateOperation(State state, ActivationOperationConfiguration activationOperationConfiguration,
                                   List<String> parametersAsString, String parameterPredicates) {
        List<Activation> activationsForOperation = configurationToActivation.get(activationOperationConfiguration.getId());
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
                activateMultiOperations(activationsForOperation, new Activation(evaluatedTime, additionalGuards, activationKind, parameters, probability, parametersAsString, parameterPredicates));
                break;
            case SINGLE:
            case SINGLE_MAX:
            case SINGLE_MIN:
                activateSingleOperations(id, opName, activationKind, new Activation(evaluatedTime, additionalGuards, activationKind, parameters, probability, parametersAsString, parameterPredicates));
                break;
        }
    }

    public boolean endingConditionReached(Trace trace) {
        return false;
    }

    public SimulationConfiguration getConfig() {
        return config;
    }

    public IntegerProperty timeProperty() {
        return time;
    }

    public int getTime() {
        return time.get();
    }

    protected abstract void run();

    public int getDelay() {
        return delay;
    }

	public List<Integer> getTimestamps() {
		return timestamps;
	}
}
