package de.prob2.ui.simulation.simulators;

import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob.animator.domainobjects.ClassicalB;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.statespace.State;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import de.prob2.ui.simulation.configuration.OperationConfiguration;
import de.prob2.ui.simulation.configuration.SimulationConfiguration;
import de.prob2.ui.simulation.configuration.SimulationFileHandler;
import de.prob2.ui.simulation.configuration.VariableChoice;
import de.prob2.ui.simulation.configuration.VariableConfiguration;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
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

    protected boolean finished;

    protected Map<String, Integer> initialOperationToRemainingTime;

    protected Map<String, Integer> operationToRemainingTime;

    protected List<OperationConfiguration> operationConfigurationsSorted;

    public AbstractSimulator() {
        this.time = new SimpleIntegerProperty(0);
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
        this.initialOperationToRemainingTime = new HashMap<>();
        this.operationToRemainingTime = new HashMap<>();
        // sort after priority
        this.operationConfigurationsSorted = config.getOperationConfigurations().stream()
                .sorted(Comparator.comparingInt(OperationConfiguration::getPriority))
                .collect(Collectors.toList());
        this.time.set(0);
        this.finished = false;
        initializeRemainingTime();
    }

    private void initializeRemainingTime() {
        config.getOperationConfigurations()
                .forEach(config -> {
                    operationToRemainingTime.put(config.getOpName(), config.getTime());
                    initialOperationToRemainingTime.put(config.getOpName(), config.getTime());
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

    public AbstractEvalResult evaluateForSimulation(State state, String formula) {
        // Note: Rodin parser does not have IF-THEN-ELSE nor REAL
        return state.eval(new ClassicalB(formula, FormulaExpand.TRUNCATE));
    }

    protected Trace simulationStep(Trace trace) {
        Trace newTrace = trace;
        State currentState = newTrace.getCurrentState();
        if(currentState.isInitialised()) {
            boolean endingTimeReached = config.getEndingTime() > 0 && time.get() >= config.getEndingTime();
            boolean endingConditionReached = endingConditionReached(newTrace);
            if (endingTimeReached || endingConditionReached) {
                finishSimulation();
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

        if(!config.getStartingCondition().isEmpty()) {
            StateSpace stateSpace = newTrace.getStateSpace();
            currentState = newTrace.getCurrentState();
            AbstractEvalResult startingResult = evaluateForSimulation(currentState, config.getStartingCondition());
            // TODO: Model Checking for goal
        }
        return newTrace;
    }

    protected Trace executeOperations(Trace trace) {
        Trace newTrace = trace;
        for(OperationConfiguration opConfig : operationConfigurationsSorted) {
            //select operations where time <= 0
            if(operationToRemainingTime.get(opConfig.getOpName()) > 0) {
                continue;
            }
            if (endingConditionReached(newTrace)) {
                break;
            }
            newTrace = executeOperation(opConfig, newTrace);
        }
        return newTrace;
    }

    private boolean endingConditionReached(Trace trace) {
        if(!config.getEndingCondition().isEmpty()) {
            AbstractEvalResult endingConditionEvalResult = evaluateForSimulation(trace.getCurrentState(), config.getEndingCondition());
            return "TRUE".equals(endingConditionEvalResult.toString());
        }
        return false;
    }

    protected abstract boolean chooseNextOperation(OperationConfiguration opConfig, Trace trace);

    protected Trace executeOperation(OperationConfiguration opConfig, Trace trace) {
        String opName = opConfig.getOpName();
        operationToRemainingTime.computeIfPresent(opName, (k, v) -> initialOperationToRemainingTime.get(opName));
        return executeNextOperation(opConfig, trace);
    }

    protected abstract Trace executeNextOperation(OperationConfiguration opConfig, Trace trace);

    protected void delayRemainingTime(OperationConfiguration opConfig) {
        Map<String, Integer> delay = opConfig.getDelay();
        if(delay != null) {
            for (String key : delay.keySet()) {
                operationToRemainingTime.computeIfPresent(key, (k, v) -> Math.max(v, delay.get(key)));
            }
        }
    }

    private String joinPredicateFromConfig(State currentState, List<VariableChoice> configs) {
        if(configs == null) {
            return "1=1";
        } else {
            return configs.stream()
                    .map(VariableChoice::getChoice)
                    .map(choice -> chooseVariableValues(currentState, choice))
                    .collect(Collectors.joining(" & "));
        }
    }

    protected Trace executeBeforeInitialisation(String operation, List<VariableChoice> configs, State currentState, Trace trace) {
        Transition nextTransition = currentState.findTransition(operation, joinPredicateFromConfig(currentState, configs));
        return trace.add(nextTransition);
    }

    protected abstract String chooseVariableValues(State currentState, List<VariableConfiguration> choice);

    public SimulationConfiguration getConfig() {
        return config;
    }

    public IntegerProperty timeProperty() {
        return time;
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
