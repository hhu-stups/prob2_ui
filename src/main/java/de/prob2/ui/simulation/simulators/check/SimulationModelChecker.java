package de.prob2.ui.simulation.simulators.check;

import de.prob.animator.command.GetOperationByPredicateCommand;
import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob.animator.domainobjects.ClassicalB;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.formula.PredicateBuilder;
import de.prob.statespace.State;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Transition;
import de.prob2.ui.simulation.configuration.OperationConfiguration;
import de.prob2.ui.simulation.configuration.SimulationConfiguration;
import de.prob2.ui.simulation.configuration.SimulationFileHandler;
import de.prob2.ui.simulation.configuration.VariableChoice;
import de.prob2.ui.simulation.configuration.VariableConfiguration;
import de.prob2.ui.simulation.simulators.AbstractSimulator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

public class SimulationModelChecker {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimulationModelChecker.class);

    public enum ModelCheckResult {
        NOT_FINISHED, SUCCESS, FAIL
    }

    private SimulationConfiguration config;

    private int interval;

    private Map<String, Integer> initialOperationToRemainingTime;

    private final StateSpace stateSpace;

    private ModelCheckResult result;

    public SimulationModelChecker(StateSpace stateSpace) {
        this.stateSpace = stateSpace;
        this.result = ModelCheckResult.NOT_FINISHED;
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

        Optional<Integer> result = relevantTimes.stream().reduce(SimulationModelChecker::gcd);
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
                .forEach(opConfig -> initialOperationToRemainingTime.put(opConfig.getOpName(), opConfig.getTime()));
    }

    private Map<String, Integer> updateRemainingTime(Map<String, Integer> operationToRemainingTime) {
        Map<String, Integer> newOperationToRemainingTime = new HashMap<>(operationToRemainingTime);
        for(String key : newOperationToRemainingTime.keySet()) {
            newOperationToRemainingTime.computeIfPresent(key, (k, v) -> Math.max(0, v - interval));
        }
        return newOperationToRemainingTime;
    }

    private Map<String, Integer> delayRemainingTime(OperationConfiguration opConfig, Map<String, Integer> operationToRemainingTime) {
        Map<String, Integer> delay = opConfig.getDelay();
        Map<String, Integer> newOperationToRemainingTime = new HashMap<>(operationToRemainingTime);
        if(delay != null) {
            for (String key : delay.keySet()) {
                newOperationToRemainingTime.computeIfPresent(key, (k, v) -> Math.max(v, delay.get(key)));
            }
        }
        return newOperationToRemainingTime;
    }

    // TODO: Implement endingCondition and endingTime

    public void check() {
        // TODO: Maybe consider time
        int time = 0;
        SimulationState root = new SimulationState(stateSpace.getRoot(), new HashMap<>(initialOperationToRemainingTime));
        Queue<SimulationState> queue = new LinkedList<>();
        queue.add(root);

        Set<SimulationState> visitedStates = new HashSet<>();
        visitedStates.add(root);

        ModelCheckResult result = ModelCheckResult.NOT_FINISHED;

        while(!queue.isEmpty()) {
            SimulationState nextState = queue.remove();
            State currentState = nextState.getBState();
            if(!currentState.isInvariantOk()) {
                result = ModelCheckResult.FAIL;
                break;
            }
            List<SimulationState> newSimulationStates = new ArrayList<>();
            if(!currentState.isInitialised()) {
                List<String> nextTransitions = currentState.getTransitions().stream().map(Transition::getName).collect(Collectors.toList());
                if(nextTransitions.contains("$setup_constants")) {
                    newSimulationStates = exploreBeforeInitialisation("$setup_constants", currentState, config.getSetupConfigurations());
                } else if(nextTransitions.contains("$initialise_machine")) {
                    newSimulationStates = exploreBeforeInitialisation("$initialise_machine", currentState, config.getInitialisationConfigurations());
                }
            } else {
                newSimulationStates = exploreNextTimeStep(nextState, config.getOperationConfigurations());
            }
            queue.addAll(newSimulationStates.stream()
                    .filter(state -> !visitedStates.contains(state))
                    .collect(Collectors.toList()));
            visitedStates.addAll(newSimulationStates);
            System.out.println(visitedStates.size());
        }
        if(result == ModelCheckResult.NOT_FINISHED) {
            this.result = ModelCheckResult.SUCCESS;
        }
    }

    private List<SimulationState> exploreBeforeInitialisation(String operationName, State state, List<VariableChoice> configurations) {
        List<SimulationState> newStates = new ArrayList<>();
        if(configurations == null) {
            newStates = state.getTransitions().stream()
                    .map(Transition::getDestination)
                    .map(dest -> new SimulationState(dest, new HashMap<>(initialOperationToRemainingTime)))
                    .collect(Collectors.toList());
        } else {
            List<List<VariableConfiguration>> combinations = new ArrayList<>();
            for(VariableChoice choice : configurations) {
                List<VariableConfiguration> varConfigurations = choice.getChoice();
                combinations = buildNextCombinations(state, combinations, varConfigurations);
            }
            for(List<VariableConfiguration> combination : combinations) {
                Map<String, String> allValues = new HashMap<>();
                for(VariableConfiguration configuration : combination) {
                    allValues.putAll(configuration.getValues());
                }
                PredicateBuilder predicateBuilder = new PredicateBuilder();
                predicateBuilder.addMap(allValues);
                String predicate = predicateBuilder.toString();
                GetOperationByPredicateCommand cmd = new GetOperationByPredicateCommand(stateSpace, state.getId(), operationName, new ClassicalB(predicate, FormulaExpand.EXPAND), 1);
                stateSpace.execute(cmd);
                if(!cmd.hasErrors()) {
                    newStates.add(new SimulationState(cmd.getNewTransitions().get(0).getDestination(), new HashMap<>(initialOperationToRemainingTime)));
                }
            }
        }
        return newStates;
    }

    // TODO: Links to previous state to detect counterexamples in breadth-first
    private List<SimulationState> exploreNextTimeStep(SimulationState state, List<OperationConfiguration> opConfigurations) {
        Map<String, Integer> newRemainingTime = updateRemainingTime(state.getOperationToRemainingTime());
        State bState = state.getBState();

        List<SimulationState> tmpNewStates = new ArrayList<>();
        tmpNewStates.add(new SimulationState(bState, newRemainingTime));

        List<OperationConfiguration> nextOperations = opConfigurations.stream()
                .filter(opConfig -> newRemainingTime.get(opConfig.getOpName()) <= 0)
                .sorted(Comparator.comparingInt(OperationConfiguration::getPriority)).collect(Collectors.toList());

        for (OperationConfiguration opConfig : nextOperations) {
            String opProbability = bState.eval(opConfig.getProbability(), FormulaExpand.EXPAND).toString();
            if ("1.0".equals(opProbability)) {
                //If probability is 1.0 then execute operation definitely
                tmpNewStates = buildNewStates(opConfig, tmpNewStates, newRemainingTime, true);
            } else if (!"0.0".equals(opProbability)) {
                //If probability is between 0.0 and 1.0 then add to branch where operation is executed, and where it is not executed
                tmpNewStates = buildNewStates(opConfig, tmpNewStates, newRemainingTime, false);
            }
        }

        List<SimulationState> newStates = new ArrayList<>();
        for(SimulationState tmpState : tmpNewStates) {
            Map<String, Integer> finalRemainingTime = new HashMap<>(tmpState.getOperationToRemainingTime());
            for(String key : finalRemainingTime.keySet()) {
                if(finalRemainingTime.get(key) <= 0) {
                    finalRemainingTime.computeIfPresent(key, (k, v) -> initialOperationToRemainingTime.get(key));
                }
            }
            newStates.add(new SimulationState(tmpState.getBState(), finalRemainingTime));
        }
        return newStates;
    }

    private List<SimulationState> buildNewStates(OperationConfiguration opConfig, List<SimulationState> states, Map<String, Integer> newRemainingTime, boolean executeDefinitely) {
        String opName = opConfig.getOpName();
        List<SimulationState> newStates = new ArrayList<>();
        Map<String, Integer> finalRemainingTime = new HashMap<>(newRemainingTime);
        finalRemainingTime.computeIfPresent(opName, (k, v) -> initialOperationToRemainingTime.get(opName));
        if (!executeDefinitely) {
            for (SimulationState state : states) {
                newStates.add(new SimulationState(state.getBState(), finalRemainingTime));
            }
        }
        for (SimulationState state : states) {
            if (state.getOperationToRemainingTime().get(opName) > 0) {
                newStates.add(state);
                continue;
            }
            State bState = state.getBState();

            List<VariableChoice> variableChoices = opConfig.getVariableChoices();
            // TODO: Handle parameters and non-determinism
            Map<String, Integer> finalOperationToRemainingTime = delayRemainingTime(opConfig, finalRemainingTime);

            List<Transition> transitions = bState.getTransitions().stream().filter(trans -> trans.getName().equals(opName)).collect(Collectors.toList());
            if(transitions.isEmpty()) {
                newStates.add(state);
            } else {
                transitions.stream()
                        .map(Transition::getDestination)
                        .forEach(succeedingState -> newStates.add(new SimulationState(succeedingState, finalOperationToRemainingTime)));
            }
        }
        return newStates;
    }

    private List<List<VariableConfiguration>> buildNextCombinations(State state, List<List<VariableConfiguration>> combinations, List<VariableConfiguration> nextConfigurations) {
        List<List<VariableConfiguration>> newCombinations = new ArrayList<>();
        if(combinations.isEmpty()) {
            for(VariableConfiguration nextConfiguration : nextConfigurations) {
                AbstractEvalResult evalResult = state.eval(nextConfiguration.getProbability(), FormulaExpand.EXPAND);
                double probability = Double.parseDouble(evalResult.toString());
                if(probability > 0.0) {
                    newCombinations.add(Collections.singletonList(nextConfiguration));
                }
            }
        } else {
            for(List<VariableConfiguration> combination : combinations) {
                for(VariableConfiguration nextConfiguration : nextConfigurations) {
                    AbstractEvalResult evalResult = state.eval(nextConfiguration.getProbability(), FormulaExpand.EXPAND);
                    double probability = Double.parseDouble(evalResult.toString());
                    if(probability > 0.0) {
                        List<VariableConfiguration> newCombination = new ArrayList<>(combination);
                        newCombination.add(nextConfiguration);
                        newCombinations.add(newCombination);
                    }
                }
            }
        }
        return newCombinations;
    }

    public ModelCheckResult getResult() {
        return result;
    }
}
