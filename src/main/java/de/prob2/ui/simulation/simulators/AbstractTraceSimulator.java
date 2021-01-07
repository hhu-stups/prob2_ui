package de.prob2.ui.simulation.simulators;

import de.prob.animator.command.ExecuteOperationException;
import de.prob.animator.command.GetOperationByPredicateCommand;
import de.prob.animator.domainobjects.AbstractEvalResult;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class AbstractTraceSimulator extends AbstractSimulator implements ITraceChecker {

    protected final Trace trace;

    protected final ReplayTrace replayTrace;

    protected int counter;

    public AbstractTraceSimulator(Trace trace, ReplayTrace replayTrace) {
        this.trace = new Trace(trace.getStateSpace());
        this.replayTrace = replayTrace;
        this.counter = 0;
    }

    @Override
    protected boolean chooseNextOperation(OperationConfiguration opConfig, Trace trace) {
        String opName = opConfig.getOpName();
        State currentState = trace.getCurrentState();

        double probability = cache.readProbabilityWithCaching(currentState, opConfig);

        List<String> enabledOperations = trace.getNextTransitions().stream()
                .map(Transition::getName)
                .collect(Collectors.toList());
        boolean equalsNextOperation = opConfig.getOpName().equals(replayTrace.getPersistentTrace().getTransitionList().get(counter).getOperationName());
        return (Math.abs(probability - 1.0) < 0.0001 && !equalsNextOperation) || (equalsNextOperation &&  probability > 0.0) && enabledOperations.contains(opName);
    }


    @Override
    protected String chooseVariableValues(State currentState, Map<String, Object> values) {
        PersistentTrace persistentTrace = replayTrace.getPersistentTrace();
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
        String opName = opConfig.getOpName();
        Map<String, Object> values = opConfig.getVariableChoices();
        State currentState = trace.getCurrentState();
        Trace newTrace = trace;

        PersistentTrace persistentTrace = replayTrace.getPersistentTrace();
        List<PersistentTransition> transitionList = persistentTrace.getTransitionList();
        PersistentTransition persistentTransition = transitionList.get(counter);

        if (!chooseNextOperation(opConfig, trace)) {
            return trace;
        }

        if(values == null) {

            List<Transition> transitions = cache.readTransitionsWithCaching(currentState, opConfig).stream()
                    .filter(trans -> trans.getName().equals(opName) && opName.equals(persistentTransition.getOperationName()))
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
                delayRemainingTime(opConfig);
            }
        } else {
            State finalCurrentState = newTrace.getCurrentState();
            String predicate = chooseVariableValues(finalCurrentState, values);
            if(finalCurrentState.getStateSpace().isValidOperation(finalCurrentState, opName, predicate)) {
                Transition transition = finalCurrentState.findTransition(opName, predicate);
                newTrace = newTrace.add(transition);
                delayRemainingTime(opConfig);
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
