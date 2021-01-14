package de.prob2.ui.simulation.simulators;

import de.prob.animator.command.ExecuteOperationException;
import de.prob.animator.command.GetOperationByPredicateCommand;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.check.tracereplay.ITraceChecker;
import de.prob.check.tracereplay.PersistentTrace;
import de.prob.check.tracereplay.PersistentTransition;
import de.prob.check.tracereplay.TraceReplay;
import de.prob.formula.PredicateBuilder;
import de.prob.statespace.State;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import de.prob2.ui.animation.tracereplay.ReplayTrace;
import de.prob2.ui.simulation.configuration.OperationConfiguration;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class AbstractTraceSimulator extends AbstractSimulator implements ITraceChecker {

    protected final Trace trace;

    protected final PersistentTrace persistentTrace;

    protected int counter;

    public AbstractTraceSimulator(Trace trace, ReplayTrace replayTrace) {
        this.trace = new Trace(trace.getStateSpace());
        this.persistentTrace = replayTrace.getPersistentTrace();
        this.counter = 0;
    }

    public AbstractTraceSimulator(Trace trace, PersistentTrace persistentTrace) {
        this.trace = new Trace(trace.getStateSpace());
        this.persistentTrace = persistentTrace;
        this.counter = 0;
    }

    @Override
    protected String chooseVariableValues(State currentState, Map<String, Object> values) {
        List<PersistentTransition> transitionList = persistentTrace.getTransitionList();
        PersistentTransition persistentTransition = transitionList.get(counter);

        String predicate = null;

        if(values == null) {
            PredicateBuilder predicateBuilder = new PredicateBuilder();
            buildPredicateFromTrace(predicateBuilder, persistentTransition);
            predicate = predicateBuilder.toString();
        } else {
            List<Map<String, String>> valueCombinations = buildValueCombinations(currentState, values);
            for(Map<String, String> combination : valueCombinations) {
                PredicateBuilder predicateBuilder = new PredicateBuilder();
                buildPredicateFromTrace(predicateBuilder, persistentTransition);
                predicateBuilder.addMap(combination);

                StateSpace stateSpace = currentState.getStateSpace();
                final IEvalElement pred = stateSpace.getModel().parseFormula(predicateBuilder.toString(), FormulaExpand.TRUNCATE);
                final GetOperationByPredicateCommand command = new GetOperationByPredicateCommand(stateSpace,
                        currentState.getId(), persistentTransition.getOperationName(), pred, 1);
                try {
                    stateSpace.execute(command);
                    if(!command.hasErrors()) {
                        predicate = predicateBuilder.toString();
                    }
                } catch (ExecuteOperationException e) {
                    continue;
                }
            }
        }
        if(predicate == null) {
            this.finished = false;
        }
        return predicate;
    }

    @Override
    public Trace executeNextOperation(OperationConfiguration opConfig, Trace trace) {
        State currentState = trace.getCurrentState();
        boolean execute = false;
        String chosenOp = "";
        Map<String, Object> values = null;
        Map<String, Integer> delay = null;

        //check whether operation is executable and decide based on sampled value between 0 and 1 and calculated probability whether it should be executed
        for(int i = 0; i < opConfig.getOpName().size(); i++) {
            chosenOp = opConfig.getOpName().get(i);
            double evalProbability = cache.readProbabilityWithCaching(currentState, chosenOp, opConfig.getProbability().get(i));
            List<String> enabledOperations = trace.getNextTransitions().stream()
                    .map(Transition::getName)
                    .collect(Collectors.toList());
            boolean equalsNextOperation = chosenOp.equals(persistentTrace.getTransitionList().get(counter).getOperationName());
            boolean operationScheduled = operationToRemainingTime.get(chosenOp) == 0;
            if(operationScheduled) {
                final String finalChosenOp = chosenOp;
                operationToRemainingTime.computeIfPresent(chosenOp, (k, v) -> initialOperationToRemainingTime.get(finalChosenOp));
            }
            boolean chooseOperation = operationScheduled && ((Math.abs(evalProbability - 1.0) < 0.0001 && !equalsNextOperation) || (equalsNextOperation && evalProbability > 0.0) && enabledOperations.contains(chosenOp));
            // TODO: What if sum of scheduled operations has 100% probability and is not equal next operation in trace
            if(chooseOperation) {
                if(opConfig.getVariableChoices() != null) {
                    values = opConfig.getVariableChoices().get(i);
                }
                if(opConfig.getDelay() != null) {
                    delay = opConfig.getDelay().get(i);
                }
                execute = true;
                break;
            }
        }
        if(!execute) {
            return trace;
        }


        List<PersistentTransition> transitionList = persistentTrace.getTransitionList();
        PersistentTransition persistentTransition = transitionList.get(counter);

        Trace newTrace = trace;

        if(values == null) {
            String finalChosenOp = chosenOp;
            List<Transition> transitions = cache.readTransitionsWithCaching(currentState, finalChosenOp).stream()
                    .filter(trans -> trans.getName().equals(finalChosenOp) && finalChosenOp.equals(persistentTransition.getOperationName()))
                    .collect(Collectors.toList());
            if(!transitions.isEmpty()) {
                PredicateBuilder predicateBuilder = new PredicateBuilder();
                buildPredicateFromTrace(predicateBuilder, persistentTransition);
                StateSpace stateSpace = trace.getStateSpace();
                final IEvalElement pred = stateSpace.getModel().parseFormula(predicateBuilder.toString(), FormulaExpand.TRUNCATE);
                final GetOperationByPredicateCommand command = new GetOperationByPredicateCommand(stateSpace,
                        currentState.getId(), persistentTransition.getOperationName(), pred, 1);
                stateSpace.execute(command);
                newTrace = newTrace.add(command.getNewTransitions().get(0));
                delayRemainingTime(delay);
            }
        } else {
            State finalCurrentState = newTrace.getCurrentState();
            String predicate = chooseVariableValues(finalCurrentState, values);
            if(finalCurrentState.getStateSpace().isValidOperation(finalCurrentState, chosenOp, predicate)) {
                Transition transition = finalCurrentState.findTransition(chosenOp, predicate);
                newTrace = newTrace.add(transition);
                delayRemainingTime(delay);
            }
        }
        return newTrace;
    }

    public void buildPredicateFromTrace(PredicateBuilder predicateBuilder, PersistentTransition persistentTransition) {
        if (persistentTransition.getParameters() != null) {
            predicateBuilder.addMap(persistentTransition.getParameters());
        }
        if (persistentTransition.getDestinationStateVariables() != null) {
            predicateBuilder.addMap(persistentTransition.getDestinationStateVariables());
        }
    }

    @Override
    protected Trace executeBeforeInitialisation(String operation, Map<String, Object> values, State currentState, Trace trace) {
        Trace res = super.executeBeforeInitialisation(operation, values, currentState, trace);
        if(res.getTransitionList().size() > trace.getTransitionList().size()) {
            counter = res.getTransitionList().size();
        }
        return res;
    }

    @Override
    protected Trace executeOperation(OperationConfiguration opConfig, Trace trace) {
        Trace res = super.executeOperation(opConfig, trace);
        if(res.getTransitionList().size() > trace.getTransitionList().size()) {
            counter = res.getTransitionList().size();
        }
        return res;
    }

    @Override
    public boolean endingConditionReached(Trace trace) {
        return counter == persistentTrace.getTransitionList().size();
    }

    @Override
    public void updateProgress(double value, Map<String, Object> replayInformation) {

    }

    @Override
    public void setResult(boolean success, Map<String, Object> replayInformation) {

    }

    @Override
    public void afterInterrupt() {

    }

    @Override
    public void showError(TraceReplay.TraceReplayError errorType, Map<String, Object> replayInformation) {

    }

}
