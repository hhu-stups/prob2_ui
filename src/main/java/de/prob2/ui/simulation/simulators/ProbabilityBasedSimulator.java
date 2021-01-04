package de.prob2.ui.simulation.simulators;

import com.github.krukow.clj_lang.PersistentVector;
import de.prob.animator.command.GetOperationByPredicateCommand;
import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.statespace.State;
import de.prob.statespace.Trace;
import de.prob.statespace.TraceElement;
import de.prob.statespace.Transition;
import de.prob2.ui.simulation.configuration.OperationConfiguration;
import de.prob2.ui.simulation.configuration.VariableChoice;
import de.prob2.ui.simulation.configuration.VariableConfiguration;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

public abstract class ProbabilityBasedSimulator extends AbstractSimulator {

    private static final Map<String, Map<String, Double>> probabilityCache = new HashMap<>();

    private static final Map<String, Map<String, List<Transition>>> transitionCache = new HashMap<>();

    @Override
    protected boolean chooseNextOperation(OperationConfiguration opConfig, Trace trace) {
        State currentState = trace.getCurrentState();
        double ranDouble = Math.random();
        String stateID = currentState.getId();
        String opName = opConfig.getOpName();
        double evalProbability = -1.0;
        if(probabilityCache.containsKey(stateID)) {
            if(probabilityCache.get(stateID).containsKey(opName)) {
                evalProbability = probabilityCache.get(stateID).get(opName);
            } else {
                AbstractEvalResult evalResult = evaluateForSimulation(currentState, opConfig.getProbability());
                evalProbability = Double.parseDouble(evalResult.toString());
                probabilityCache.get(stateID).put(opName, evalProbability);
            }
        } else {
            probabilityCache.put(stateID, new HashMap<>());
            AbstractEvalResult evalResult = evaluateForSimulation(currentState, opConfig.getProbability());
            evalProbability = Double.parseDouble(evalResult.toString());
            probabilityCache.get(stateID).put(opName, evalProbability);
        }

        return evalProbability > ranDouble;
    }

    @Override
    public String chooseVariableValues(State currentState, List<VariableConfiguration> choice) {
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
        StringBuilder conjuncts = new StringBuilder();
        for(Iterator<String> it = chosenValues.keySet().iterator(); it.hasNext();) {
            String next = it.next();
            AbstractEvalResult evalResult = evaluateForSimulation(currentState, chosenValues.get(next));
            conjuncts.append(next);
            conjuncts.append(" = ");
            conjuncts.append(evalResult.toString());
            if(it.hasNext()) {
                conjuncts.append(" & ");
            }
        }
        return conjuncts.toString();
    }

    private void loadTransitionsInCache(State currentState, String opName) {
        String stateID = currentState.getId();
        if(!transitionCache.containsKey(stateID)) {
            transitionCache.put(stateID, new HashMap<>());
        }
        transitionCache.get(stateID).put(opName, currentState.getOutTransitions().stream()
                .filter(trans -> trans.getName().equals(opName))
                .collect(Collectors.toList()));
    }

    @Override
    public Trace executeNextOperation(OperationConfiguration opConfig, Trace trace) {
        String opName = opConfig.getOpName();
        State currentState = trace.getCurrentState();
        if(!transitionCache.containsKey(currentState.getId()) || !transitionCache.get(currentState.getId()).containsKey(opName)) {
            loadTransitionsInCache(currentState, opName);
        }
        List<Transition> transitions = transitionCache.get(currentState.getId()).get(opName);
        //check whether operation is executable and calculate probability whether it should be executed
        if (transitions.isEmpty() || !chooseNextOperation(opConfig, trace)) {
            return trace;
        }
        List<VariableChoice> choices = opConfig.getVariableChoices();
        Trace newTrace = trace;
        if(choices == null) {
            Random rand = new Random();
            Transition transition = transitions.get(rand.nextInt(transitions.size()));
            newTrace = appendTrace(newTrace, transition);
            delayRemainingTime(opConfig);
        } else {
            State finalCurrentState = newTrace.getCurrentState();
            String predicate = choices.stream()
                    .map(choice -> chooseVariableValues(finalCurrentState, choice.getChoice()))
                    .collect(Collectors.joining(" & "));
            final IEvalElement pred = newTrace.getModel().parseFormula(predicate, FormulaExpand.TRUNCATE);
            final GetOperationByPredicateCommand command = new GetOperationByPredicateCommand(finalCurrentState.getStateSpace(), finalCurrentState.getId(), opName, pred, 1);
            finalCurrentState.getStateSpace().execute(command);
            if (!command.hasErrors()) {
                Transition transition = command.getNewTransitions().get(0);
                newTrace = appendTrace(newTrace, transition);
                delayRemainingTime(opConfig);
            }
        }
        return newTrace;
    }

    public Trace appendTrace(Trace trace, Transition transition) {
        TraceElement current = trace.getCurrent();
        PersistentVector<Transition> transitionList = (PersistentVector<Transition>) trace.getTransitionList();
        current = new TraceElement(transition, current);
        transitionList = transitionList.assocN(transitionList.size(), transition);
        return new Trace(trace.getStateSpace(), current, transitionList, trace.getUUID());
    }
}
