package de.prob2.ui.simulation.simulators;

import de.prob.statespace.State;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import de.prob2.ui.simulation.configuration.ActivationConfiguration;
import de.prob2.ui.simulation.configuration.TimingConfiguration;
import de.prob2.ui.simulation.configuration.SimulationConfiguration;
import de.prob2.ui.simulation.configuration.SimulationFileHandler;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class AbstractSimulator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractSimulator.class);

    protected SimulationConfiguration config;

    protected IntegerProperty time;

    protected int delay;

    protected int stepCounter;

    protected boolean finished;

    protected Map<String, List<Activation>> operationToActivation;

    protected Map<String, TimingConfiguration.ActivationKind> operationToActivationKind;

    protected List<TimingConfiguration> timingConfigurationsSorted;

    protected SimulatorCache cache;

    public AbstractSimulator() {
        this.time = new SimpleIntegerProperty(0);
        this.cache = new SimulatorCache();
    }

    public void initSimulator(File configFile) {
        this.config = null;
        try {
            this.config = SimulationFileHandler.constructConfigurationFromJSON(configFile);
        } catch (IOException e) {
            LOGGER.debug("Tried to load simulation configuration file");
            //TODO: Implement alert
            return;
        }
        resetSimulator();
    }

    public void resetSimulator() {
        this.operationToActivation = new HashMap<>();
        this.operationToActivationKind = new HashMap<>();
        this.time.set(0);
        this.finished = false;
        this.stepCounter = 0;
        if(config != null) {
            // sort after priority
            this.timingConfigurationsSorted = config.getTimingConfigurations().stream()
                    .sorted(Comparator.comparingInt(TimingConfiguration::getPriority))
                    .collect(Collectors.toList());
            initializeRemainingTime();
        }
    }

    private void initializeRemainingTime() {
        config.getTimingConfigurations()
                .forEach(config -> {
                    operationToActivation.put(config.getOpName(), new ArrayList<>());
                    operationToActivationKind.put(config.getOpName(), config.getActivationKind());
                });
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
                newTrace = executeBeforeInitialisation("$setup_constants", config.getTimingConfigurations(), currentState, newTrace);
            }
            currentState = newTrace.getCurrentState();
            nextTransitions = newTrace.getNextTransitions().stream().map(Transition::getName).collect(Collectors.toList());
            if(nextTransitions.contains("$initialise_machine")) {
                newTrace = executeBeforeInitialisation("$initialise_machine", config.getTimingConfigurations(), currentState, newTrace);
            }
        }
        stepCounter = newTrace.getTransitionList().size();
        return newTrace;
    }

    protected Trace executeOperations(Trace trace) {
        Trace newTrace = trace;
        for(TimingConfiguration opConfig : timingConfigurationsSorted) {
            if (endingConditionReached(newTrace)) {
                break;
            }
            newTrace = executeOperation(opConfig, newTrace);
        }
        return newTrace;
    }

    public abstract boolean endingConditionReached(Trace trace);

    protected Trace executeOperation(TimingConfiguration opConfig, Trace trace) {
        return executeNextOperation(opConfig, trace);
    }

    protected abstract Trace executeNextOperation(TimingConfiguration opConfig, Trace trace);

    protected void activateMultiOperations(List<Activation> activationsForOperation, Activation activation) {
        int insertionIndex = 0;
        while(insertionIndex < activationsForOperation.size() &&
              activation.getTime() >= activationsForOperation.get(insertionIndex).getTime()) {
            insertionIndex++;
        }
        activationsForOperation.add(insertionIndex, activation);
    }

    protected void activateOperations(Map<String, List<ActivationConfiguration>> activation) {
        if(activation != null) {
            for (String key : activation.keySet()) {
                // TODO: Add parameters to activations
                List<Activation> activationsForOperation = operationToActivation.get(key);
                TimingConfiguration.ActivationKind activationKind = operationToActivationKind.get(key);
                List<ActivationConfiguration> activationConfigurations = activation.get(key);
                activationConfigurations.forEach(activationConfiguration -> activateOperation(activationKind, activationsForOperation, activationConfiguration));
            }
        }
    }

    private void activateOperation(TimingConfiguration.ActivationKind activationKind, List<Activation> activationsForOperation, ActivationConfiguration activationConfiguration) {
        if(activationsForOperation.isEmpty()) {
            activationsForOperation.add(new Activation(activationConfiguration));
        } else {
            switch (activationKind) {
                case MULTI:
                    activateMultiOperations(activationsForOperation, new Activation(activationConfiguration));
                    break;
                case SINGLE_MIN: {
                    Activation activationForOperation = activationsForOperation.get(0);
                    int otherActivationTime = activationForOperation.getTime();
                    if (activationConfiguration.getTime() < otherActivationTime) {
                        activationsForOperation.clear();
                        activationsForOperation.add(new Activation(activationConfiguration));
                    }
                    break;
                }
                case SINGLE_MAX: {
                    Activation activationForOperation = activationsForOperation.get(0);
                    int otherActivationTime = activationForOperation.getTime();
                    if (activationConfiguration.getTime() > otherActivationTime) {
                        activationsForOperation.clear();
                        activationsForOperation.add(new Activation(activationConfiguration));
                    }
                    break;
                }
                case SINGLE:
                default:
                    break;
            }
        }
    }



    private String joinPredicateFromValues(State currentState, Map<String, String> values) {
        if(values == null) {
            return "1=1";
        } else {
            return chooseVariableValues(currentState, values);
        }
    }

    protected Trace executeBeforeInitialisation(String operation, List<TimingConfiguration> opConfigurations, State currentState, Trace trace) {
    	List<TimingConfiguration> opConfigs = opConfigurations.stream()
				.filter(config -> operation.equals(config.getOpName()))
				.collect(Collectors.toList());
    	String predicate = "1=1";
    	if(!opConfigs.isEmpty()) {
    	    TimingConfiguration opConfig = opConfigs.get(0);
            predicate = joinPredicateFromValues(currentState, opConfig.getVariableChoices());
            activateOperations(opConfig.getActivation());
		}
    	updateDelay();
        Transition nextTransition = currentState.findTransition(operation, predicate);
        return trace.add(nextTransition);
    }

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
