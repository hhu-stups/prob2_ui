package de.prob2.ui.simulation;

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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

public abstract class AbstractSimulator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractSimulator.class);

    protected SimulationConfiguration config;

    protected IntegerProperty time;

    protected int interval;

    protected boolean finished;

    protected Map<String, Integer> initialOperationToRemainingTime;

    protected Map<String, Integer> operationToRemainingTime;

    public AbstractSimulator() {
        this.time = new SimpleIntegerProperty(0);
    }

    protected void initSimulator(File configFile) {
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
        this.time.set(0);
        this.finished = false;
        calculateInterval();
        initializeRemainingTime();
    }

    private void calculateInterval() {
        List<Integer> relevantTimes = new ArrayList<>(config.getOperationConfigurations().stream()
                .map(OperationConfiguration::getTime)
                .collect(Collectors.toList()));
        for(OperationConfiguration opConfig : config.getOperationConfigurations()) {
            if(opConfig.getDelay() != null) {
                relevantTimes.addAll(opConfig.getDelay().values());
            }
        }

        Optional<Integer> result = relevantTimes.stream().reduce(AbstractSimulator::gcd);
        result.ifPresent(integer -> interval = integer);
    }

    private static int gcd(int a, int b) {
        if(b == 0) {
            return a;
        }
        return gcd(b, a % b);
    }

    private void initializeRemainingTime() {
        config.getOperationConfigurations()
                .forEach(config -> {
                    operationToRemainingTime.put(config.getOpName(), config.getTime());
                    initialOperationToRemainingTime.put(config.getOpName(), config.getTime());
                });
    }

    public void updateRemainingTime() {
        this.time.set(this.time.get() + this.interval);
        for(String key : operationToRemainingTime.keySet()) {
            operationToRemainingTime.computeIfPresent(key, (k, v) -> v - interval);
        }
    }

    public AbstractEvalResult evaluateForSimulation(State state, String formula) {
        // Note: Rodin parser does not have IF-THEN-ELSE nor REAL
        return state.eval(new ClassicalB(formula, FormulaExpand.EXPAND));
    }

    protected Trace simulationStep(Trace trace) {
        Trace newTrace = trace;
        State currentState = newTrace.getCurrentState();
        if(currentState.isInitialised()) {
            boolean endingTimeReached = config.getEndingTime() > 0 && time.get() >= config.getEndingTime();
            boolean endingConditionReached = false;
            if(!config.getEndingCondition().isEmpty()) {
                AbstractEvalResult endingConditionEvalResult = evaluateForSimulation(currentState, config.getEndingCondition());
                endingConditionReached = "TRUE".equals(endingConditionEvalResult.toString());
            }
            if (endingTimeReached || endingConditionReached) {
                finishSimulation();
                return newTrace;
            }
            updateRemainingTime();
            newTrace = executeOperations(newTrace);
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
        //select operations where time <= 0
        List<OperationConfiguration> nextOperations = config.getOperationConfigurations().stream()
                .filter(opConfig -> operationToRemainingTime.get(opConfig.getOpName()) <= 0)
                .collect(Collectors.toList());


        //sort operations after priority (less number means higher priority)
        nextOperations.sort(Comparator.comparingInt(OperationConfiguration::getPriority));

        Trace newTrace = trace;

        for(OperationConfiguration opConfig : nextOperations) {
            if(!config.getEndingCondition().isEmpty()) {
                AbstractEvalResult endingConditionEvalResult = evaluateForSimulation(newTrace.getCurrentState(), config.getEndingCondition());
                if ("TRUE".equals(endingConditionEvalResult.toString())) {
                    return newTrace;
                }
            }
            newTrace = executeOperation(opConfig, newTrace);
        }
        return newTrace;
    }

    protected abstract boolean chooseNextOperation(OperationConfiguration opConfig, Trace trace);

    protected Trace executeOperation(OperationConfiguration opConfig, Trace trace) {

        String opName = opConfig.getOpName();
        //time for next execution has been delayed by a previous transition
        if(operationToRemainingTime.get(opName) > 0) {
            return trace;
        }

        operationToRemainingTime.computeIfPresent(opName, (k, v) -> initialOperationToRemainingTime.get(opName));

        Trace newTrace = trace;
        State currentState = trace.getCurrentState();

        //check whether operation is executable and calculate probability whether it should be executed
        if(chooseNextOperation(opConfig, newTrace)) {
            List<VariableChoice> choices = opConfig.getVariableChoices();

            //execute operation and append to trace
            if(choices == null) {
                List<Transition> transitions = currentState.getTransitions().stream()
                        .filter(trans -> trans.getName().equals(opName))
                        .collect(Collectors.toList());
                if(transitions.size() > 0) {
                    Random rand = new Random();
                    Transition transition = transitions.get(rand.nextInt(transitions.size()));
                    newTrace = newTrace.add(transition);
                    delayRemainingTime(opConfig);
                }
            } else {
                State finalCurrentState = trace.getCurrentState();
                String predicate = choices.stream()
                        .map(VariableChoice::getChoice)
                        .map(choice -> chooseVariableValues(finalCurrentState, choice))
                        .collect(Collectors.joining(" & "));
                if(finalCurrentState.getStateSpace().isValidOperation(finalCurrentState, opName, predicate)) {
                    Transition transition = finalCurrentState.findTransition(opName, predicate);
                    newTrace = newTrace.add(transition);
                    delayRemainingTime(opConfig);
                }
            }
        }
        return newTrace;
    }

    private void delayRemainingTime(OperationConfiguration opConfig) {
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

}
