package de.prob2.ui.simulation.simulators;

import com.github.krukow.clj_lang.PersistentVector;
import de.prob.animator.command.GetOperationByPredicateCommand;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.statespace.State;
import de.prob.statespace.Trace;
import de.prob.statespace.TraceElement;
import de.prob.statespace.Transition;
import de.prob2.ui.simulation.configuration.OperationConfiguration;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

public abstract class ProbabilityBasedSimulator extends AbstractSimulator {

    private Random random = new Random(System.nanoTime());

    @Override
    public String chooseVariableValues(State currentState, Map<String, Object> values) {
        StringBuilder conjuncts = new StringBuilder();
        for(Iterator<String> it = values.keySet().iterator(); it.hasNext();) {
            String key = it.next();
            Object value = values.get(key);
            String evalResult;
            if(value instanceof List) {
                List<String> list = (List<String>) value;
                String randomElement = list.get(random.nextInt(list.size()));
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
        State currentState = trace.getCurrentState();
        double probabilityMinimum = 0.0;
        boolean execute = false;
        String chosenOp = "";
        Map<String, Object> values = null;
        List<Transition> transitions = null;
        Map<String, Integer> delay = null;
        //check whether operation is executable and decide based on sampled value between 0 and 1 and calculated probability whether it should be executed
        for(int i = 0; i < opConfig.getOpName().size(); i++) {
            chosenOp = opConfig.getOpName().get(i);
            double evalProbability = cache.readProbabilityWithCaching(currentState, chosenOp, opConfig.getProbability().get(i));
            double randomDouble = random.nextDouble();
            boolean opScheduled = operationToRemainingTime.get(chosenOp) == 0;
            if(opScheduled) {
                final String finalChosenOp = chosenOp;
                operationToRemainingTime.computeIfPresent(chosenOp, (k, v) -> initialOperationToRemainingTime.get(finalChosenOp));
            }
            if(randomDouble > probabilityMinimum && randomDouble < probabilityMinimum + evalProbability) {
                //select operation also only if time = 0
                if(!opScheduled) {
                    return trace;
                }
                // TODO: Refactor
                transitions = cache.readTransitionsWithCaching(currentState, chosenOp);
                if (transitions.isEmpty()) {
                    return trace;
                }
                if(opConfig.getVariableChoices() != null) {
                    values = opConfig.getVariableChoices().get(i);
                }
                if(opConfig.getDelay() != null) {
                    delay = opConfig.getDelay().get(i);
                }
                execute = true;
                break;
            }
            probabilityMinimum += evalProbability;
        }
        if(!execute) {
            return trace;
        }

        Trace newTrace = trace;
        if(values == null) {
            Transition transition = transitions.get(random.nextInt(transitions.size()));
            newTrace = appendTrace(newTrace, transition);
            delayRemainingTime(delay);
        } else {
            State finalCurrentState = newTrace.getCurrentState();
            String predicate = chooseVariableValues(finalCurrentState, values);
            final IEvalElement pred = newTrace.getModel().parseFormula(predicate, FormulaExpand.TRUNCATE);
            final GetOperationByPredicateCommand command = new GetOperationByPredicateCommand(finalCurrentState.getStateSpace(), finalCurrentState.getId(), chosenOp, pred, 1);
            finalCurrentState.getStateSpace().execute(command);
            if (!command.hasErrors()) {
                Transition transition = command.getNewTransitions().get(0);
                newTrace = appendTrace(newTrace, transition);
                delayRemainingTime(delay);
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
