package de.prob2.ui.simulation;

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
import de.prob2.ui.simulation.configuration.VariableChoice;
import de.prob2.ui.simulation.configuration.VariableConfiguration;

import java.util.HashMap;
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

        AbstractEvalResult evalResult = evaluateForSimulation(currentState, opConfig.getProbability());

        List<String> enabledOperations = trace.getNextTransitions().stream()
                .map(Transition::getName)
                .collect(Collectors.toList());
        boolean equalsNextOperation = opConfig.getOpName().equals(replayTrace.getPersistentTrace().getTransitionList().get(counter).getOperationName());
        double probability = Double.parseDouble(evalResult.toString());
        return (Math.abs(probability - 1.0) < 0.0001 && !equalsNextOperation) || (equalsNextOperation &&  probability > 0.0) && enabledOperations.contains(opName);
    }

    @Override
    protected String chooseVariableValues(State currentState, List<VariableConfiguration> choice) {
        PersistentTrace persistentTrace = replayTrace.getPersistentTrace();
        List<PersistentTransition> transitionList = persistentTrace.getTransitionList();
        PersistentTransition persistentTransition = transitionList.get(counter);

        String predicate = null;

        if(config == null) {
            PredicateBuilder predicateBuilder = new PredicateBuilder();
            buildPredicateFromTrace(predicateBuilder, persistentTransition);
            predicate = predicateBuilder.toString();
        } else {
            for (VariableConfiguration config : choice) {
                AbstractEvalResult probabilityResult = evaluateForSimulation(currentState, config.getProbability());
                double probability = Double.parseDouble(probabilityResult.toString());
                if (probability > 0.0) {
                    PredicateBuilder predicateBuilder = new PredicateBuilder();
                    Map<String, String> values = new HashMap<>(config.getValues());
                    for (String key : values.keySet()) {
                        values.computeIfPresent(key, (k, v) -> evaluateForSimulation(currentState, values.get(key)).toString());
                    }
                    predicateBuilder.addMap(values);
                    buildPredicateFromTrace(predicateBuilder, persistentTransition);
                    StateSpace stateSpace = currentState.getStateSpace();
                    final IEvalElement pred = stateSpace.getModel().parseFormula(predicateBuilder.toString(), FormulaExpand.EXPAND);
                    final GetOperationByPredicateCommand command = new GetOperationByPredicateCommand(stateSpace,
                            currentState.getId(), persistentTransition.getOperationName(), pred, 1);
                    try {
                        stateSpace.execute(command);
                        if(!command.hasErrors()) {
                            predicate = predicateBuilder.toString();
                        }
                    } catch (ExecuteOperationException e) {
                        System.out.println("TRACE REPLAY IN SIMULATION ERROR");
                    }
                }
            }
        }
        if(predicate == null) {
            this.finished = false;
        }
        return predicate;
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
    protected Trace executeBeforeInitialisation(String operation, List<VariableChoice> configs, State currentState, Trace trace) {
        Trace res = super.executeBeforeInitialisation(operation, configs, currentState, trace);
        if(res.getTransitionList().size() > trace.getTransitionList().size()) {
            counter++;
        }
        return res;
    }

    @Override
    protected Trace executeOperation(OperationConfiguration opConfig, Trace trace) {
        Trace res = super.executeOperation(opConfig, trace);
        if(res.getTransitionList().size() > trace.getTransitionList().size()) {
            counter++;
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
