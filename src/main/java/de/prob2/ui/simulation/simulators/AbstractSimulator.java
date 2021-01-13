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

    protected Map<String, Integer> initialOperationToRemainingTime;

    protected Map<String, Integer> operationToRemainingTime;

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
        this.initialOperationToRemainingTime = new HashMap<>();
        this.operationToRemainingTime = new HashMap<>();
        this.time.set(0);
        this.finished = false;
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
                        operationToRemainingTime.put(op, config.getTime());
                        initialOperationToRemainingTime.put(op, config.getTime());
                    }
                });
        updateDelay();
    }

    public void updateRemainingTime() {
        updateRemainingTime(this.delay);
    }

    public void updateRemainingTime(int delay) {
        this.time.set(this.time.get() + delay);
        for(String key : operationToRemainingTime.keySet()) {
            operationToRemainingTime.computeIfPresent(key, (k, v) -> Math.max(0, v - delay));
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
                newTrace = executeBeforeInitialisation("$setup_constants", config.getSetupConfigurations(), currentState, newTrace);
            }
            currentState = newTrace.getCurrentState();
            nextTransitions = newTrace.getNextTransitions().stream().map(Transition::getName).collect(Collectors.toList());
            if(nextTransitions.contains("$initialise_machine")) {
                newTrace = executeBeforeInitialisation("$initialise_machine", config.getInitialisationConfigurations(), currentState, newTrace);
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

    protected void delayRemainingTime(Map<String, Integer> delay) {
        if(delay != null) {
            for (String key : delay.keySet()) {
                operationToRemainingTime.computeIfPresent(key, (k, v) -> Math.max(v, delay.get(key)));
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

    protected Trace executeBeforeInitialisation(String operation, Map<String, Object> values, State currentState, Trace trace) {
        Transition nextTransition = currentState.findTransition(operation, joinPredicateFromValues(currentState, values));
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
        this.delay = operationToRemainingTime.values().stream().reduce(Integer.MAX_VALUE, Math::min);
    }
}
