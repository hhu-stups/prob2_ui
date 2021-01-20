package de.prob2.ui.simulation.simulators;

import de.prob.statespace.State;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import de.prob2.ui.simulation.configuration.OperationConfiguration;
import de.prob2.ui.simulation.configuration.SimulationConfiguration;
import de.prob2.ui.simulation.configuration.SimulationFileHandler;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
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

    protected Map<String, List<Integer>> operationToActivationTimes;

    protected Map<String, OperationConfiguration.ActivationKind> operationToActivationKind;

    protected List<OperationConfiguration> operationConfigurationsSorted;

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
        this.operationToActivationTimes = new HashMap<>();
        this.operationToActivationKind = new HashMap<>();
        this.time.set(0);
        this.finished = false;
        this.stepCounter = 0;
        if(config != null) {
            // sort after priority
            this.operationConfigurationsSorted = config.getOperationConfigurations().stream()
                    .sorted(Comparator.comparingInt(OperationConfiguration::getPriority))
                    .collect(Collectors.toList());
            initializeRemainingTime();
        }
    }

    private void initializeRemainingTime() {
        config.getOperationConfigurations()
                .forEach(config -> {
                    for(String op : config.getOpName()) {
						operationToActivationTimes.put(op, new ArrayList<>());
                        operationToActivationKind.put(op, config.getActivationKind());
                    }
                });
        updateDelay();
    }

    public void updateRemainingTime() {
        updateRemainingTime(this.delay);
    }

    public void updateRemainingTime(int delay) {
        this.time.set(this.time.get() + delay);
        for(String key : operationToActivationTimes.keySet()) {
			operationToActivationTimes.computeIfPresent(key, (k, v) -> operationToActivationTimes.get(key).stream().map(time -> time - delay).collect(Collectors.toList()));
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

    protected void activateOperations(Map<String, Integer> activation) {
        if(activation != null) {
            for (String key : activation.keySet()) {
                List<Integer> activationTimes = operationToActivationTimes.get(key);
                OperationConfiguration.ActivationKind activationKind = operationToActivationKind.get(key);
                if(activationKind == OperationConfiguration.ActivationKind.MULTI) {
                    activationTimes.add(activation.get(key));
                } else {
                    if(activationTimes.isEmpty()) {
                        activationTimes.add(activation.get(key));
                    } else {
                        Integer otherActivationTime = activationTimes.get(0);
                        if(activationKind == OperationConfiguration.ActivationKind.SINGLE_MAX) {
                            activationTimes.clear();
                            activationTimes.add(Math.max(otherActivationTime, activation.get(key)));
                        } else if(activationKind == OperationConfiguration.ActivationKind.SINGLE_MIN) {
                            activationTimes.clear();
                            activationTimes.add(Math.min(otherActivationTime, activation.get(key)));
                        }
                    }
                }
            }
        }
    }

    private String joinPredicateFromValues(State currentState, Map<String, Object> values) {
        if(values == null) {
            return "1=1";
        } else {
            return chooseVariableValues(currentState, values);
        }
    }

    protected Trace executeBeforeInitialisation(String operation, List<OperationConfiguration> opConfigurations, State currentState, Trace trace) {
    	List<OperationConfiguration> opConfigs = opConfigurations.stream()
				.filter(config -> operation.equals(config.getOpName().get(0)))
				.collect(Collectors.toList());
    	String predicate = "";
    	if(opConfigs.isEmpty()) {
    		predicate = "1=1";
		} else {
    		if(opConfigs.get(0).getVariableChoices() == null) {
    			predicate = "1=1";
			} else {
				predicate = joinPredicateFromValues(currentState, opConfigs.get(0).getVariableChoices().get(0));
			}
    		if(opConfigs.get(0).getActivation() != null) {
                activateOperations(opConfigs.get(0).getActivation().get(0));
            }
		}
    	updateDelay();
        Transition nextTransition = currentState.findTransition(operation, predicate);
        return trace.add(nextTransition);
    }

    protected List<Map<String, String>> buildValueCombinations(State currentState, Map<String, Object> values) {
        List<Map<String, String>> result = new ArrayList<>();
        for(Iterator<String> it = values.keySet().iterator(); it.hasNext();) {
            String key = it.next();
            Object value = values.get(key);
            List<String> valueList = new ArrayList<>();
            if(value instanceof List) {
                valueList = (List<String>) value;
            } else if(value instanceof String) {
                valueList = Arrays.asList((String) value);
            }
            if(result.isEmpty()) {
                for(String element : valueList) {
                    Map<String, String> initialMap = new HashMap<>();
                    initialMap.put(key, cache.readValueWithCaching(currentState, element));
                    result.add(initialMap);
                }
            } else {
                List<Map<String, String>> oldResult = result;
                result = new ArrayList<>();
                for(Map<String, String> map : oldResult) {
                    for(String element : valueList) {
                        Map<String, String> newMap = new HashMap<>(map);
                        newMap.put(key, cache.readValueWithCaching(currentState, element));
                        result.add(newMap);
                    }
                }

            }
        }
        return result;
    }

    protected abstract String chooseVariableValues(State currentState, Map<String, Object> values);

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
    	for(List<Integer> times : operationToActivationTimes.values()) {
    		for(int time : times) {
    			if(time < delay) {
    				delay = time;
				}
			}
		}
    	this.delay = delay;
    }
}
