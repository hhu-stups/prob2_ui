package de.prob2.ui.simulation.simulators;

import com.github.krukow.clj_lang.PersistentVector;
import de.prob.animator.command.GetOperationByPredicateCommand;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.statespace.State;
import de.prob.statespace.Trace;
import de.prob.statespace.TraceElement;
import de.prob.statespace.Transition;
import de.prob2.ui.simulation.configuration.ActivationConfiguration;
import de.prob2.ui.simulation.configuration.TimingConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
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

    public Map<String, Object> chooseProbabilistic(Activation activation, State currentState) {
        Map<String, Object> values = null;

        Object probability = activation.getProbability();
        if(probability == null) {
            return values;
        }
        //choose between non-deterministic assigned variables
        if(probability instanceof String) {
            if("uniform".equals(probability.toString())) {
                return null;
            }
        } else {
            Map<String, Object> probabilityMap = (Map<String, Object>) probability;
            values = new HashMap<>();
            for(String variable : probabilityMap.keySet()) {
                // TODO: cache probability
                double probabilityMinimum = 0.0;
                Object probabilityValue = probabilityMap.get(variable);
                Map<String, String> probabilityValueMap = (Map<String, String>) probabilityValue;
                double randomDouble = random.nextDouble();
                for(String value : probabilityValueMap.keySet()) {
                    String valueProbability = probabilityValueMap.get(value);
                    double evalProbability = Double.parseDouble(currentState.eval(valueProbability, FormulaExpand.TRUNCATE).toString());
                    if(randomDouble > probabilityMinimum && randomDouble < probabilityMinimum + evalProbability) {
                        String evalValue = currentState.eval(value, FormulaExpand.TRUNCATE).toString();
                        values.put(variable, evalValue);
                    }
                    probabilityMinimum += evalProbability;
                }
            }
        }

        return values;
    }

    @Override
    public Trace executeNextOperation(TimingConfiguration timingConfig, Trace trace) {
        State currentState = trace.getCurrentState();
        String chosenOp = timingConfig.getOpName();
        Map<String, ActivationConfiguration> activationConfiguration = null;

        List<Transition> transitions = cache.readTransitionsWithCaching(currentState, chosenOp);

        List<Activation> activationForOperation = operationToActivation.get(chosenOp);
        List<Activation> activationForOperationCopy = new ArrayList<>(activationForOperation);

        Trace newTrace = trace;
        for(Activation activation : activationForOperationCopy) {
            //select operation only if its time is 0
            if(activation.getTime() > 0) {
                break;
            }
            activationForOperation.remove(activation);

            // TODO
            String additionalGuards = timingConfig.getAdditionalGuards();
            String additionalGuardsResult = "TRUE";
            if (additionalGuards != null) {
                additionalGuardsResult = currentState.eval(additionalGuards, FormulaExpand.TRUNCATE).toString();
            }
            boolean execute = false;
            if (!transitions.isEmpty() && "TRUE".equals(additionalGuardsResult)) {
                if (timingConfig.getActivation() != null) {
                    activationConfiguration = timingConfig.getActivation();
                }
                execute = true;
            }

            if (!execute) {
                continue;
            }

            Map<String, Object> values = chooseProbabilistic(activation, currentState);

            if (values == null) {
                Transition transition = transitions.get(random.nextInt(transitions.size()));
                newTrace = appendTrace(newTrace, transition);
                activateOperations(activationConfiguration);
            } else {
                State finalCurrentState = newTrace.getCurrentState();
                String predicate = chooseVariableValues(finalCurrentState, values);
                final IEvalElement pred = newTrace.getModel().parseFormula(predicate, FormulaExpand.TRUNCATE);
                final GetOperationByPredicateCommand command = new GetOperationByPredicateCommand(finalCurrentState.getStateSpace(), finalCurrentState.getId(), chosenOp, pred, 1);
                finalCurrentState.getStateSpace().execute(command);
                if (!command.hasErrors()) {
                    Transition transition = command.getNewTransitions().get(0);
                    newTrace = appendTrace(newTrace, transition);
                    activateOperations(activationConfiguration);
                }
            }
        }
        stepCounter = newTrace.getTransitionList().size();
        return newTrace;
    }

    public Trace appendTrace(Trace trace, Transition transition) {
        TraceElement current = trace.getCurrent();
        PersistentVector<Transition> transitionList = (PersistentVector<Transition>) trace.getTransitionList();
        current = new TraceElement(transition, current);
        transitionList = transitionList.assocN(transitionList.size(), transition);
        return new Trace(trace.getStateSpace(), current, transitionList, trace.getUUID());
    }

    @Override
    public boolean endingConditionReached(Trace trace) {
        // TODO: Implement simulation with ending condition without Monte Carlo simulation
        return false;
    }
}
