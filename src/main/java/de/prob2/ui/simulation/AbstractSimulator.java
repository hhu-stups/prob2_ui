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

    public Trace simulationStep(Trace trace) {
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
            newTrace = executeOperations(currentState, newTrace);
        }
        return newTrace;
    }

    protected Trace setupBeforeSimulation(Trace trace) {
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
                newTrace = executeBeforeInitialisation("$initialise_machine", config.getSetupConfigurations(), currentState, newTrace);
            }
        }

        if(!config.getStartingCondition().isEmpty()) {
            StateSpace stateSpace = newTrace.getStateSpace();
            currentState = newTrace.getCurrentState();
            AbstractEvalResult startingResult = evaluateForSimulation(currentState, config.getStartingCondition());
            // Model Checking for goal
			/*if("FALSE".equals(startingResult.toString())) {
				final IModelCheckListener mcListener = new IModelCheckListener() {
					@Override
					public void updateStats(final String jobId, final long timeElapsed, final IModelCheckingResult result, final StateSpaceStats stats) {

					}

					@Override
					public void isFinished(final String jobId, final long timeElapsed, final IModelCheckingResult result, final StateSpaceStats stats) {
						if (result instanceof ITraceDescription) {
							Trace newTrace = ((ITraceDescription) result).getTrace(stateSpace);
							String firstID = newTrace.getTransitionList().get(0).getId()
							currentTrace.set();
						}
					}
				};
				ConsistencyChecker checker = new ConsistencyChecker(stateSpace, new ModelCheckingOptions().breadthFirst(true).recheckExisting(true).checkGoal(true), new ClassicalB(config.getStartingCondition(), FormulaExpand.EXPAND), mcListener);
				checker.call();
			}*/
        }
        return newTrace;
    }

    protected Trace executeOperations(State currentState, Trace trace) {
        //1. select operations where time <= 0
        List<OperationConfiguration> nextOperations = config.getOperationConfigurations().stream()
                .filter(opConfig -> operationToRemainingTime.get(opConfig.getOpName()) <= 0)
                .collect(Collectors.toList());


        //2. sort operations after priority (less number means higher priority)
        nextOperations.sort(Comparator.comparingInt(OperationConfiguration::getPriority));

        Trace newTrace = trace;
        State newCurrentState = currentState;

        for(OperationConfiguration opConfig : nextOperations) {
            if(!config.getEndingCondition().isEmpty()) {
                AbstractEvalResult endingConditionEvalResult = evaluateForSimulation(newCurrentState, config.getEndingCondition());
                if ("TRUE".equals(endingConditionEvalResult.toString())) {
                    return newTrace;
                }
            }
            String opName = opConfig.getOpName();
            //time for next execution has been delayed by a previous transition
            if(operationToRemainingTime.get(opName) > 0) {
                continue;
            }

            operationToRemainingTime.computeIfPresent(opName, (k, v) -> initialOperationToRemainingTime.get(opName));

            double ranDouble = Math.random();

            AbstractEvalResult evalResult = evaluateForSimulation(newCurrentState, opConfig.getProbability());

            //3. calculate probability for each operation whether it should be executed
            if(Double.parseDouble(evalResult.toString()) > ranDouble) {
                List<String> enabledOperations = newTrace.getNextTransitions().stream()
                        .map(Transition::getName)
                        .collect(Collectors.toList());

                //4. check whether operation is executable
                if (enabledOperations.contains(opName)) {
                    List<VariableChoice> choices = opConfig.getVariableChoices();

                    //5. execute operation and append to trace
                    boolean operationExecuted = false;
                    if(choices == null) {
                        List<Transition> transitions = newCurrentState.getTransitions().stream()
                                .filter(trans -> trans.getName().equals(opName))
                                .collect(Collectors.toList());
                        Random rand = new Random();
                        Transition transition = transitions.get(rand.nextInt(transitions.size()));
                        newTrace = newTrace.add(transition);
                        newCurrentState = newTrace.getCurrentState();
                        operationExecuted = true;
                    } else {
                        State finalNewCurrentState = newCurrentState;
                        String predicate = choices.stream()
                                .map(VariableChoice::getChoice)
                                .map(choice -> chooseVariableValues(finalNewCurrentState, choice))
                                .collect(Collectors.joining(" & "));
                        if(newCurrentState.getStateSpace().isValidOperation(newCurrentState, opName, predicate)) {
                            Transition transition = newCurrentState.findTransition(opName, predicate);
                            newTrace = newTrace.add(transition);
                            newCurrentState = newTrace.getCurrentState();
                            operationExecuted = true;
                        }
                    }
                    Map<String, Integer> delay = opConfig.getDelay();
                    if(operationExecuted && delay != null) {
                        for (String key : delay.keySet()) {
                            operationToRemainingTime.computeIfPresent(key, (k, v) -> Math.max(v, delay.get(key)));
                        }
                    }
                }
            }
        }
        return newTrace;
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

    private Trace executeBeforeInitialisation(String operation, List<VariableChoice> configs, State currentState, Trace trace) {
        Transition nextTransition = currentState.findTransition(operation, joinPredicateFromConfig(currentState, configs));
        return trace.add(nextTransition);
    }

    private String chooseVariableValues(State currentState, List<VariableConfiguration> choice) {
        double ranDouble = Math.random();
        double minimumProbability = 0.0;
        VariableConfiguration chosenConfiguration = choice.get(0);

        //Choose configuration for execution
        for(VariableConfiguration config : choice) {
            AbstractEvalResult probabilityResult = evaluateForSimulation(currentState, config.getProbability());
            minimumProbability += Double.parseDouble(probabilityResult.toString());
            chosenConfiguration = config;
            if(minimumProbability > ranDouble) {
                break;
            }
        }

        Map<String, String> chosenValues = chosenConfiguration.getValues();
        List<String> conjuncts = new ArrayList<>();
        for(String key : chosenValues.keySet()) {
            AbstractEvalResult evalResult = evaluateForSimulation(currentState, chosenValues.get(key));
            conjuncts.add(key + " = " + evalResult.toString());
        }
        return String.join(" & ", conjuncts);
    }

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
