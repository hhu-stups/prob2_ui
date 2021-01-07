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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

public abstract class ProbabilityBasedSimulator extends AbstractSimulator {

    @Override
    protected boolean chooseNextOperation(OperationConfiguration opConfig, Trace trace) {
        State currentState = trace.getCurrentState();
        double ranDouble = Math.random();
        double evalProbability = cache.readProbabilityWithCaching(currentState, opConfig);
        return evalProbability > ranDouble;
    }

    @Override
    public String chooseVariableValues(State currentState, Map<String, Object> values) {
        StringBuilder conjuncts = new StringBuilder();
        for(Iterator<String> it = values.keySet().iterator(); it.hasNext();) {
            String key = it.next();
            Object value = values.get(key);
            String evalResult;
            if(value instanceof List) {
                Random rand = new Random();
                List<String> list = (List<String>) value;
                String randomElement = list.get(rand.nextInt(list.size()));
                evalResult = cache.readValueWithCaching(currentState, randomElement);
            } else {
                //Otherwise it is a String
                evalResult = cache.readValueWithCaching(currentState, (String) value);
            }
            conjuncts.append(key);
            conjuncts.append(" = ");
            conjuncts.append(evalResult);
            if(it.hasNext()) {
                conjuncts.append(" & ");
            }
        }
        return conjuncts.toString();
    }

    @Override
    public Trace executeNextOperation(OperationConfiguration opConfig, Trace trace) {
        String opName = opConfig.getOpName();
        State currentState = trace.getCurrentState();
        List<Transition> transitions = cache.readTransitionsWithCaching(currentState, opConfig);
        //check whether operation is executable and calculate probability whether it should be executed
        if (transitions.isEmpty() || !chooseNextOperation(opConfig, trace)) {
            return trace;
        }
        Map<String, Object> values = opConfig.getVariableChoices();
        Trace newTrace = trace;
        if(values == null) {
            Random rand = new Random();
            Transition transition = transitions.get(rand.nextInt(transitions.size()));
            newTrace = appendTrace(newTrace, transition);
            delayRemainingTime(opConfig);
        } else {
            State finalCurrentState = newTrace.getCurrentState();
            String predicate = chooseVariableValues(finalCurrentState, values);
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
