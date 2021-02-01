package de.prob2.ui.simulation.simulators;

import de.prob.statespace.State;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.simulation.configuration.ActivationConfiguration;
import de.prob2.ui.simulation.configuration.ActivationOperationConfiguration;
import de.prob2.ui.simulation.configuration.SimulationConfigurationChecker;
import de.prob2.ui.simulation.configuration.SimulationConfiguration;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class AbstractSimulator {

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

    public AbstractSimulator(final CurrentTrace currentTrace) {
    	this.currentTrace = currentTrace;
        this.time = new SimpleIntegerProperty(0);
        this.cache = new SimulatorCache();
        this.traceListener = (observable, from, to) -> {
			if(config != null && to != null && to.getStateSpace() != null) {
				checkConfiguration(to.getStateSpace());
				currentTrace.removeListener(traceListener);
			}
		};
    }


    public void initSimulator(File configFile) throws IOException {
        this.config = SimulationFileHandler.constructConfigurationFromJSON(configFile);
        if(currentTrace.get() != null && currentTrace.getStateSpace() != null) {
        	checkConfiguration(currentTrace.getStateSpace());
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
        this.time.set(0);
        this.stepCounter = 0;
        if(config != null) {
            // sort after priority
            this.activationConfigurationsSorted = config.getActivationConfigurations().stream()
                    .filter(activationConfiguration -> activationConfiguration instanceof ActivationOperationConfiguration)
                    .map(activationConfiguration -> (ActivationOperationConfiguration) activationConfiguration)
                    .sorted(Comparator.comparingInt(ActivationOperationConfiguration::getPriority))
                    .collect(Collectors.toList());
            activationConfigurationMap = new HashMap<>();
            config.getActivationConfigurations().forEach(activationConfiguration -> activationConfigurationMap.put(activationConfiguration.getId(), activationConfiguration));
            operationToActivations = new HashMap<>();
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
            initializeRemainingTime();
            currentTrace.removeListener(traceListener);
        }
    }

    private void initializeRemainingTime() {
        activationConfigurationsSorted.forEach(config -> configurationToActivation.put(config.getId(), new ArrayList<>()));
        updateDelay();
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

    protected Trace simulationStep(Trace trace) {
        Trace newTrace = trace;
        State currentState = newTrace.getCurrentState();
        if(currentState.isInitialised()) {
            if(endingConditionReached(newTrace)) {
                return newTrace;
            }
            updateRemainingTime();
            newTrace = executeOperations(newTrace);
            updateDelay();
        }
        return newTrace;
    }

    public Trace setupBeforeSimulation(Trace trace) {
        Trace newTrace = trace;
        State currentState = newTrace.getCurrentState();
        if(!currentState.isInitialised()) {
            List<String> nextTransitions = trace.getNextTransitions().stream().map(Transition::getName).collect(Collectors.toList());
            if(nextTransitions.contains("$setup_constants")) {
                newTrace = executeBeforeInitialisation("$setup_constants", currentState, newTrace);
            }
            currentState = newTrace.getCurrentState();
            nextTransitions = newTrace.getNextTransitions().stream().map(Transition::getName).collect(Collectors.toList());
            if(nextTransitions.contains("$initialise_machine")) {
                newTrace = executeBeforeInitialisation("$initialise_machine", currentState, newTrace);
            }
        }
        stepCounter = newTrace.getTransitionList().size();
        return newTrace;
    }

    protected Trace executeOperations(Trace trace) {
        Trace newTrace = trace;
        for(ActivationOperationConfiguration opConfig : activationConfigurationsSorted) {
            if (endingConditionReached(newTrace)) {
                break;
            }
            newTrace = executeOperation(opConfig, newTrace);
        }
        return newTrace;
    }

    public abstract boolean endingConditionReached(Trace trace);

    protected Trace executeOperation(ActivationOperationConfiguration activationConfig, Trace trace) {
        return executeNextOperation(activationConfig, trace);
    }

    protected abstract Trace executeNextOperation(ActivationOperationConfiguration activationConfig, Trace trace);

    protected String evaluateWithParameters(State state, String expression, List<String> parametersAsString, String parameterPredicate) {
        String newExpression;
        if("1=1".equals(parameterPredicate) || parametersAsString.isEmpty()) {
            newExpression = expression;
        } else {
            newExpression = String.format("LET %s BE %s IN %s END", String.join(", ", parametersAsString), parameterPredicate, expression);
        }
        return cache.readValueWithCaching(state, newExpression);
    }


    private String joinPredicateFromValues(State currentState, Map<String, String> values) {
        if(values == null) {
            return "1=1";
        } else {
            return chooseVariableValues(currentState, values);
        }
    }

    protected Trace executeBeforeInitialisation(String operation, State currentState, Trace trace) {
    	List<ActivationOperationConfiguration> actConfigs = activationConfigurationsSorted.stream()
				.filter(config -> operation.equals(config.getOpName()))
				.collect(Collectors.toList());
    	String predicate = "1=1";
    	Trace newTrace = trace;
    	if(!actConfigs.isEmpty()) {
    	    ActivationOperationConfiguration actConfig = actConfigs.get(0);
            predicate = joinPredicateFromValues(currentState, actConfig.getParameters());
            Transition nextTransition = currentState.findTransition(operation, predicate);
            newTrace = trace.add(nextTransition);
            activateOperations(newTrace.getCurrentState(), actConfig.getActivation(), new ArrayList<>(), "1=1");
		} else {
            Transition nextTransition = currentState.findTransition(operation, predicate);
            newTrace = trace.add(nextTransition);
    	}
        updateDelay();
        return newTrace;
    }

    protected abstract void activateOperations(State state, List<String> activation, List<String> parametersAsString, String parameterPredicates);

    protected abstract String chooseVariableValues(State currentState, Map<String, String> values);

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
}
