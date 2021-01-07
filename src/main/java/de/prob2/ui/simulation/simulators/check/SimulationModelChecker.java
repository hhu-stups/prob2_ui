package de.prob2.ui.simulation.simulators.check;

import de.prob.animator.command.GetOperationByPredicateCommand;
import de.prob.animator.domainobjects.ClassicalB;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.formula.PredicateBuilder;
import de.prob.statespace.State;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Transition;
import de.prob2.ui.simulation.SimulationHelperFunctions;
import de.prob2.ui.simulation.configuration.OperationConfiguration;
import de.prob2.ui.simulation.configuration.SimulationConfiguration;
import de.prob2.ui.simulation.configuration.SimulationFileHandler;
import de.prob2.ui.simulation.simulators.SimulatorCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

public class SimulationModelChecker {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimulationModelChecker.class);

    public enum ModelCheckResult {
        NOT_FINISHED, SUCCESS, FAIL
    }

    private SimulationConfiguration config;

    private Map<String, Integer> initialOperationToRemainingTime;

    private List<OperationConfiguration> operationConfigurationsSorted;

    private final StateSpace stateSpace;

    private final SimulatorCache cache;

    private ModelCheckResult result;

    public SimulationModelChecker(StateSpace stateSpace) {
        this.stateSpace = stateSpace;
        this.result = ModelCheckResult.NOT_FINISHED;
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
        this.initialOperationToRemainingTime = new HashMap<>();
        // sort after priority
        this.operationConfigurationsSorted = config.getOperationConfigurations().stream()
                .sorted(Comparator.comparingInt(OperationConfiguration::getPriority))
                .collect(Collectors.toList());
        initializeRemainingTime();
    }

    private void initializeRemainingTime() {
        config.getOperationConfigurations()
                .forEach(opConfig -> initialOperationToRemainingTime.put(opConfig.getOpName(), opConfig.getTime()));
    }

    private Map<String, Integer> updateRemainingTime(Map<String, Integer> operationToRemainingTime, int delay) {
        Map<String, Integer> newOperationToRemainingTime = new HashMap<>(operationToRemainingTime);
        for(String key : newOperationToRemainingTime.keySet()) {
            newOperationToRemainingTime.computeIfPresent(key, (k, v) -> Math.max(0, v - delay));
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
        Thread thread = new Thread(() -> {
            try {
                stateSpace.startTransaction();
                int time = 0;
                SimulationState root = new SimulationState(stateSpace.getRoot(), new HashMap<>(initialOperationToRemainingTime));
                Queue<SimulationState> queue = new LinkedList<>();
                queue.add(root);

                Set<SimulationState> visitedStates = new HashSet<>();
                visitedStates.add(root);

                ModelCheckResult result = ModelCheckResult.NOT_FINISHED;

                while (!queue.isEmpty()) {
                    SimulationState nextState = queue.remove();
                    State currentState = nextState.getBState();
                    if (!currentState.isInvariantOk()) {
                        result = ModelCheckResult.FAIL;
                        break;
                    }
                    List<SimulationState> newSimulationStates = new ArrayList<>();
                    if (!currentState.isInitialised()) {
                        List<String> nextTransitions = currentState.getTransitions().stream().map(Transition::getName).collect(Collectors.toList());
                        if (nextTransitions.contains("$setup_constants")) {
                            newSimulationStates = exploreBeforeInitialisation("$setup_constants", currentState, config.getSetupConfigurations());
                        } else if (nextTransitions.contains("$initialise_machine")) {
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

                if (result == ModelCheckResult.NOT_FINISHED) {
                    this.result = ModelCheckResult.SUCCESS;
                }
            } finally {
                stateSpace.endTransaction();
            }
        });
        thread.start();
    }

    private List<SimulationState> exploreBeforeInitialisation(String operationName, State state, Map<String, Object> values) {
        List<SimulationState> newStates = new ArrayList<>();
        if(values == null) {
            newStates = state.getTransitions().stream()
                    .map(Transition::getDestination)
                    .map(dest -> new SimulationState(dest, new HashMap<>(initialOperationToRemainingTime)))
                    .collect(Collectors.toList());
        } else {
            List<Map<String, String>> combinations = buildValueCombinations(state, values);
            for(Map<String, String> combination : combinations) {
                PredicateBuilder predicateBuilder = new PredicateBuilder();
                predicateBuilder.addMap(combination);
                String predicate = predicateBuilder.toString();
                GetOperationByPredicateCommand cmd = new GetOperationByPredicateCommand(stateSpace, state.getId(), operationName, new ClassicalB(predicate, FormulaExpand.TRUNCATE), 1);
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
        int delay = state.getOperationToRemainingTime().values().stream().reduce(Integer.MAX_VALUE, Math::min);
        Map<String, Integer> newRemainingTime = updateRemainingTime(state.getOperationToRemainingTime(), delay);
        State bState = state.getBState();

        List<SimulationState> tmpNewStates = new ArrayList<>();
        tmpNewStates.add(new SimulationState(bState, newRemainingTime));

        for (OperationConfiguration opConfig : operationConfigurationsSorted) {
            if(newRemainingTime.get(opConfig.getOpName()) > 0) {
                continue;
            }

            double opProbability = cache.readProbabilityWithCaching(bState, opConfig);

            boolean probabilityNearlyOne = Math.abs(opProbability - 1.0) < 0.0001;
            boolean probabilityNearlyZero = opProbability < 0.0001;

            if (probabilityNearlyOne) {
                //If probability is 1.0 then execute operation definitely
                tmpNewStates = buildNewStates(opConfig, tmpNewStates, newRemainingTime, true);
            } else if (!probabilityNearlyZero) {
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
            Map<String, Object> values = opConfig.getVariableChoices();
            List<Transition> transitions = cache.readTransitionsWithCaching(bState, opConfig);
            if(transitions.isEmpty()) {
                newStates.add(state);
            } else {
                Map<String, Integer> finalOperationToRemainingTime = delayRemainingTime(opConfig, finalRemainingTime);
                if(values == null) {
                    for(Transition transition : transitions) {
                        newStates.add(new SimulationState(transition.getDestination(), finalOperationToRemainingTime));
                    }
                } else {
                    List<Map<String, String>> combinations = buildValueCombinations(bState, values);
                    for (Map<String, String> combination : combinations) {
                        PredicateBuilder predicateBuilder = new PredicateBuilder();
                        predicateBuilder.addMap(combination);
                        String predicate = predicateBuilder.toString();
                        GetOperationByPredicateCommand cmd = new GetOperationByPredicateCommand(stateSpace, bState.getId(), opName, new ClassicalB(predicate, FormulaExpand.TRUNCATE), 1);
                        stateSpace.execute(cmd);
                        if (!cmd.hasErrors()) {
                            newStates.add(new SimulationState(cmd.getNewTransitions().get(0).getDestination(), new HashMap<>(finalOperationToRemainingTime)));
                        }
                    }
                }
            }
        }
        return newStates;
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
                    initialMap.put(key, SimulationHelperFunctions.evaluateForSimulation(currentState, element).toString());
                    result.add(initialMap);
                }
            } else {
                List<Map<String, String>> oldResult = result;
                result = new ArrayList<>();
                for(Map<String, String> map : oldResult) {
                    for(String element : valueList) {
                        Map<String, String> newMap = new HashMap<>(map);
                        newMap.put(key, SimulationHelperFunctions.evaluateForSimulation(currentState, element).toString());
                        result.add(newMap);
                    }
                }

            }
        }
        return result;
    }

    public ModelCheckResult getResult() {
        return result;
    }
}
