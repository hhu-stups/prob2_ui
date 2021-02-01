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
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.simulation.configuration.ActivationConfiguration;
import de.prob2.ui.simulation.configuration.ActivationOperationConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class AbstractTraceSimulator extends AbstractSimulator implements ITraceChecker {

    protected final Trace trace;

    protected final PersistentTrace persistentTrace;

    protected int counter;

    public AbstractTraceSimulator(final CurrentTrace currentTrace, Trace trace, ReplayTrace replayTrace) {
    	super(currentTrace);
        this.trace = new Trace(trace.getStateSpace());
        this.persistentTrace = replayTrace.getPersistentTrace();
        this.counter = 0;
    }

    public AbstractTraceSimulator(final CurrentTrace currentTrace, Trace trace, PersistentTrace persistentTrace) {
    	super(currentTrace);
        this.trace = new Trace(trace.getStateSpace());
        this.persistentTrace = persistentTrace;
        this.counter = 0;
    }

    @Override
    protected String chooseVariableValues(State currentState, Map<String, String> values) {
        List<PersistentTransition> transitionList = persistentTrace.getTransitionList();
        PersistentTransition persistentTransition = transitionList.get(counter);

        String predicate = null;

        if(values == null) {
            PredicateBuilder predicateBuilder = new PredicateBuilder();
            buildPredicateFromTrace(predicateBuilder, persistentTransition);
            predicate = predicateBuilder.toString();
        } else {
            PredicateBuilder predicateBuilder = new PredicateBuilder();
            buildPredicateFromTrace(predicateBuilder, persistentTransition);
            predicateBuilder.addMap(values);

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
                //
            }
        }
        return predicate;
    }

    @Override
    public Trace executeNextOperation(ActivationOperationConfiguration timingConfig, Trace trace) {
        State currentState = trace.getCurrentState();
        //boolean execute = false;
        Map<String, String> values = null;
        List<String> activationConfiguration = null;

        //check whether operation is executable and decide based on sampled value between 0 and 1 and calculated probability whether it should be executed
        String id = timingConfig.getId();
        String chosenOp = timingConfig.getOpName();
        //double evalProbability = cache.readProbabilityWithCaching(currentState, chosenOp, opConfig.getProbability().get(i));
        List<String> enabledOperations = trace.getNextTransitions().stream()
                .map(Transition::getName)
                .collect(Collectors.toList());
        List<Activation> activationForOperation = configurationToActivation.get(id);
        List<Activation> activationForOperationCopy = new ArrayList<>(configurationToActivation.get(id));

        // TODO: Re-implement Trace Replay with new timing and probabilistic behavior
        Trace newTrace = trace;
        for(Activation activation : activationForOperationCopy) {
            if(activation.getTime() > 0) {
                break;
            }
            activationForOperation.remove(activation);

            // TODO: What if sum of scheduled operations has 100% probability and is not equal next operation in trace
            if (timingConfig.getParameters()!= null) {
                values = timingConfig.getParameters();
            }
            if (timingConfig.getActivation() != null) {
                activationConfiguration = timingConfig.getActivation();
            }
            if (!enabledOperations.contains(chosenOp)) {
                return trace;
            }


            List<PersistentTransition> transitionList = persistentTrace.getTransitionList();
            PersistentTransition persistentTransition = transitionList.get(counter);

            if (values == null) {
                String finalChosenOp = chosenOp;
                List<Transition> transitions = cache.readTransitionsWithCaching(currentState, finalChosenOp).stream()
                        .filter(trans -> trans.getName().equals(finalChosenOp) && finalChosenOp.equals(persistentTransition.getOperationName()))
                        .collect(Collectors.toList());
                if (!transitions.isEmpty()) {
                    PredicateBuilder predicateBuilder = new PredicateBuilder();
                    buildPredicateFromTrace(predicateBuilder, persistentTransition);
                    StateSpace stateSpace = trace.getStateSpace();
                    final IEvalElement pred = stateSpace.getModel().parseFormula(predicateBuilder.toString(), FormulaExpand.TRUNCATE);
                    final GetOperationByPredicateCommand command = new GetOperationByPredicateCommand(stateSpace,
                            currentState.getId(), persistentTransition.getOperationName(), pred, 1);
                    stateSpace.execute(command);
                    Transition transition = command.getNewTransitions().get(0);
                    newTrace = newTrace.add(transition);
                    // TODO: Implement directed (regarding probability) activate operations
                    //activateOperations(newTrace.getCurrentState(), activationConfiguration, transition.getParameterNames(), transition.getParameterPredicate());
                }
            } else {
                State finalCurrentState = newTrace.getCurrentState();
                String predicate = chooseVariableValues(finalCurrentState, values);
                if (finalCurrentState.getStateSpace().isValidOperation(finalCurrentState, chosenOp, predicate)) {
                    Transition transition = finalCurrentState.findTransition(chosenOp, predicate);
                    newTrace = newTrace.add(transition);
                    // TODO: Implement directed (regarding probability) activate operations
                    //activateOperations(newTrace.getCurrentState(), activationConfiguration, transition.getParameterNames(), transition.getParameterPredicate());
                }
            }
        }
        return newTrace;
    }

    @Override
    protected void activateOperations(State state, List<String> activation, List<String> parametersAsString, String parameterPredicates) {
        // TODO: Implement directed (regarding probability) activate operations
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
    protected Trace executeBeforeInitialisation(String operation, State currentState, Trace trace) {
        Trace res = super.executeBeforeInitialisation(operation, currentState, trace);
        if(res.getTransitionList().size() > trace.getTransitionList().size()) {
            counter = res.getTransitionList().size();
        }
        return res;
    }

    @Override
    protected Trace executeOperation(ActivationOperationConfiguration opConfig, Trace trace) {
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
