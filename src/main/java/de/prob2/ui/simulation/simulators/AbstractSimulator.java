package de.prob2.ui.simulation.simulators;

import de.prob.statespace.State;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.simulation.configuration.ActivationConfiguration;
import de.prob2.ui.simulation.configuration.SimulationConfigurationChecker;
import de.prob2.ui.simulation.configuration.OperationConfiguration;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class AbstractSimulator {

    protected SimulationConfiguration config;

    protected IntegerProperty time;

    protected int delay;

    protected int stepCounter;

    protected boolean finished;

    protected Map<String, List<Activation>> operationToActivation;

    protected List<OperationConfiguration> operationConfigurationsSorted;

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
        this.operationToActivation = new HashMap<>();
        this.time.set(0);
        this.finished = false;
        this.stepCounter = 0;
        if(config != null) {
            // sort after priority
            this.operationConfigurationsSorted = config.getOperationConfigurations().stream()
                    .sorted(Comparator.comparingInt(OperationConfiguration::getPriority))
                    .collect(Collectors.toList());
            initializeRemainingTime();
            currentTrace.removeListener(traceListener);
        }
    }

    private void initializeRemainingTime() {
        config.getOperationConfigurations().forEach(config -> operationToActivation.put(config.getOpName(), new ArrayList<>()));
        updateDelay();
    }

    public void updateRemainingTime() {
        updateRemainingTime(this.delay);
    }

    public void updateRemainingTime(int delay) {
        this.time.set(this.time.get() + delay);
        for(String key : operationToActivation.keySet()) {
            for(Activation activation : operationToActivation.get(key)) {
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
                newTrace = executeBeforeInitialisation("$setup_constants", config.getOperationConfigurations(), currentState, newTrace);
            }
            currentState = newTrace.getCurrentState();
            nextTransitions = newTrace.getNextTransitions().stream().map(Transition::getName).collect(Collectors.toList());
            if(nextTransitions.contains("$initialise_machine")) {
                newTrace = executeBeforeInitialisation("$initialise_machine", config.getOperationConfigurations(), currentState, newTrace);
            }
        }
        stepCounter = newTrace.getTransitionList().size();
        return newTrace;
    }

    protected Trace executeOperations(Trace trace) {
        Trace newTrace = trace;
        for(OperationConfiguration opConfig : operationConfigurationsSorted) {
            if (endingConditionReached(newTrace)) {
                break;
            }
            newTrace = executeOperation(opConfig, newTrace);
        }
        return newTrace;
    }

    public abstract boolean endingConditionReached(Trace trace);

    protected Trace executeOperation(OperationConfiguration opConfig, Trace trace) {
        return executeNextOperation(opConfig, trace);
    }

    protected abstract Trace executeNextOperation(OperationConfiguration opConfig, Trace trace);

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

    protected Trace executeBeforeInitialisation(String operation, List<OperationConfiguration> opConfigurations, State currentState, Trace trace) {
    	List<OperationConfiguration> opConfigs = opConfigurations.stream()
				.filter(config -> operation.equals(config.getOpName()))
				.collect(Collectors.toList());
    	String predicate = "1=1";
    	if(!opConfigs.isEmpty()) {
    	    OperationConfiguration opConfig = opConfigs.get(0);
            predicate = joinPredicateFromValues(currentState, opConfig.getDestState());
            activateOperations(currentState, opConfig.getActivation(), new ArrayList<>(), "1=1");
		}
    	updateDelay();
        Transition nextTransition = currentState.findTransition(operation, predicate);
        return trace.add(nextTransition);
    }

    protected abstract void activateOperations(State state, List<ActivationConfiguration> activation, List<String> parametersAsString, String parameterPredicates);

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

    protected void finishSimulation() {
        this.finished = true;
    }

    public int getDelay() {
        return delay;
    }

    public boolean isFinished() {
        return finished;
    }

    public void updateDelay() {
    	int delay = Integer.MAX_VALUE;
    	for(List<Activation> activations : operationToActivation.values()) {
    		for(Activation activation : activations) {
    			if(activation.getTime() < delay) {
    				delay = activation.getTime();
				}
			}
		}
    	this.delay = delay;
    }
}
