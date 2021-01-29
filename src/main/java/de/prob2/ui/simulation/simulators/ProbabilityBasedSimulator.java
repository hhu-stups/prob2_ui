package de.prob2.ui.simulation.simulators;

import com.github.krukow.clj_lang.PersistentVector;
import de.prob.animator.command.GetOperationByPredicateCommand;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.statespace.State;
import de.prob.statespace.Trace;
import de.prob.statespace.TraceElement;
import de.prob.statespace.Transition;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.simulation.configuration.ActivationChoiceConfiguration;
import de.prob2.ui.simulation.configuration.ActivationConfiguration;
import de.prob2.ui.simulation.configuration.ActivationOperationConfiguration;
import de.prob2.ui.simulation.configuration.OperationConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

public abstract class ProbabilityBasedSimulator extends AbstractSimulator {

    private Random random = new Random(System.nanoTime());

	public ProbabilityBasedSimulator(final CurrentTrace currentTrace) {
		super(currentTrace);
	}

	@Override
    public String chooseVariableValues(State currentState, Map<String, String> values) {
        StringBuilder conjuncts = new StringBuilder();
        for(Iterator<String> it = values.keySet().iterator(); it.hasNext();) {
            String key = it.next();
            String value = values.get(key);
            String evalResult  = cache.readValueWithCaching(currentState, value);
            conjuncts.append(key);
            conjuncts.append(" = ");
            conjuncts.append(evalResult);
            if(it.hasNext()) {
                conjuncts.append(" & ");
            }
        }
        return conjuncts.toString();
    }

    public Map<String, String> chooseParameters(Activation activation, State currentState) {
        Map<String, String> parameters = activation.getParameters();
        if(parameters == null) {
            return null;
        }
        Map<String, String> values = new HashMap<>();
        for(String parameter : parameters.keySet()) {
            String value = evaluateWithParameters(currentState, parameters.get(parameter), activation.getFiringTransitionParameters(), activation.getFiringTransitionParametersPredicate());
            values.put(parameter, value);
        }
        return values;
    }


    public Map<String, String> chooseProbabilistic(Activation activation, State currentState) {
        Map<String, String> values = null;

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
            Map<String, Map<String, String>> probabilityMap = (Map<String, Map<String, String>>) probability;
            values = new HashMap<>();
            for(String variable : probabilityMap.keySet()) {
                double probabilityMinimum = 0.0;
                Object probabilityValue = probabilityMap.get(variable);
                Map<String, String> probabilityValueMap = (Map<String, String>) probabilityValue;
                double randomDouble = random.nextDouble();
                for(String value : probabilityValueMap.keySet()) {
                    String valueProbability = probabilityValueMap.get(value);
                    double evalProbability = Double.parseDouble(cache.readValueWithCaching(currentState, valueProbability));
                    if(randomDouble > probabilityMinimum && randomDouble < probabilityMinimum + evalProbability) {
                        String evalValue = cache.readValueWithCaching(currentState, value);
                        values.put(variable, evalValue);
                    }
                    probabilityMinimum += evalProbability;
                }
            }
        }

        return values;
    }

    private boolean shouldExecuteNextOperation(State state, List<Transition> transitions, String additionalGuards) {
        String additionalGuardsResult = additionalGuards == null ? "TRUE" : cache.readValueWithCaching(state, additionalGuards);
        if (transitions.isEmpty() || "FALSE".equals(additionalGuardsResult)) {
            return false;
        }
        return true;
    }

    private Map<String, String> mergeValues(Map<String, String> values1, Map<String, String> values2) {
        if(values1 == null) {
            return values2;
        }

        if(values2 == null) {
            return values1;
        }

        Map<String, String> newValues = new HashMap<>(values1);
        newValues.putAll(values2);
        return newValues;
    }

    @Override
    public Trace executeNextOperation(OperationConfiguration timingConfig, Trace trace) {
        String chosenOp = timingConfig.getOpName();
        List<ActivationConfiguration> activationConfiguration = timingConfig.getActivation();

        List<Activation> activationForOperation = operationToActivation.get(chosenOp);
        List<Activation> activationForOperationCopy = new ArrayList<>(activationForOperation);

        Trace newTrace = trace;
        for(Activation activation : activationForOperationCopy) {
            //select operation only if its time is 0
            if(activation.getTime() > 0) {
                break;
            }
            activationForOperation.remove(activation);

            State currentState = newTrace.getCurrentState();
            List<Transition> transitions = cache.readTransitionsWithCaching(currentState, chosenOp);
            if (!shouldExecuteNextOperation(currentState, transitions, activation.getAdditionalGuards())) {
                continue;
            }
            Map<String, String> values = mergeValues(chooseProbabilistic(activation, currentState), chooseParameters(activation, currentState));

            if (values == null) {
                // TODO: uniform, refactor
                Transition transition = transitions.get(random.nextInt(transitions.size()));
                newTrace = appendTrace(newTrace, transition);
                activateOperations(newTrace.getCurrentState(), activationConfiguration, transition.getParameterNames(), transition.getParameterPredicate());
            } else {
                State finalCurrentState = newTrace.getCurrentState();
                String predicate = chooseVariableValues(finalCurrentState, values);
                final IEvalElement pred = newTrace.getModel().parseFormula(predicate, FormulaExpand.TRUNCATE);
                final GetOperationByPredicateCommand command = new GetOperationByPredicateCommand(finalCurrentState.getStateSpace(), finalCurrentState.getId(), chosenOp, pred, 1);
                finalCurrentState.getStateSpace().execute(command);
                if (!command.hasErrors()) {
                    Transition transition = command.getNewTransitions().get(0);
                    newTrace = appendTrace(newTrace, transition);
                    activateOperations(newTrace.getCurrentState(), activationConfiguration, transition.getParameterNames(), transition.getParameterPredicate());
                }
            }
        }
        stepCounter = newTrace.getTransitionList().size();
        return newTrace;
    }

    protected void activateMultiOperations(List<Activation> activationsForOperation, Activation activation) {
        int insertionIndex = 0;
        while(insertionIndex < activationsForOperation.size() &&
                activation.getTime() >= activationsForOperation.get(insertionIndex).getTime()) {
            insertionIndex++;
        }
        activationsForOperation.add(insertionIndex, activation);
    }

    @Override
    public void activateOperations(State state, List<ActivationConfiguration> activation, List<String> parametersAsString, String parameterPredicates) {
        if(activation != null) {
            activation.forEach(activationConfiguration -> handleOperationConfiguration(state, activationConfiguration, parametersAsString, parameterPredicates));
        }
    }

    private void handleOperationConfiguration(State state, ActivationConfiguration activationConfiguration, List<String> parametersAsString, String parameterPredicates) {
        if(activationConfiguration instanceof ActivationChoiceConfiguration) {
            chooseOperation(state, (ActivationChoiceConfiguration) activationConfiguration, parametersAsString, parameterPredicates);
        } else if(activationConfiguration instanceof ActivationOperationConfiguration) {
            activateOperation(state, (ActivationOperationConfiguration) activationConfiguration, parametersAsString, parameterPredicates);
        }
    }

    private void chooseOperation(State state, ActivationChoiceConfiguration activationChoiceConfiguration,
                                 List<String> parametersAsString, String parameterPredicates) {
        double probabilityMinimum = 0.0;
        double randomDouble = random.nextDouble();
        for(int i = 0; i < activationChoiceConfiguration.getActivations().size(); i++) {
            ActivationConfiguration activationConfiguration = activationChoiceConfiguration.getActivations().get(i);
            double evalProbability = Double.parseDouble(cache.readValueWithCaching(state, activationChoiceConfiguration.getProbability().get(i)));
            if(randomDouble > probabilityMinimum && randomDouble < probabilityMinimum + evalProbability) {
                handleOperationConfiguration(state, activationConfiguration, parametersAsString, parameterPredicates);
                break;
            }
            probabilityMinimum += evalProbability;
        }
    }

    private void activateOperation(State state, ActivationOperationConfiguration activationOperationConfiguration,
                                   List<String> parametersAsString, String parameterPredicates) {
        List<Activation> activationsForOperation = operationToActivation.get(activationOperationConfiguration.getOpName());
        String time = activationOperationConfiguration.getTime();
        ActivationOperationConfiguration.ActivationKind activationKind = activationOperationConfiguration.getActivationKind();
        String additionalGuards = activationOperationConfiguration.getAdditionalGuards();
        Map<String, String> parameters = activationOperationConfiguration.getParameters();
        Object probability = activationOperationConfiguration.getProbability();
        int evaluatedTime = Integer.parseInt(evaluateWithParameters(state, time, parametersAsString, parameterPredicates));

        if(activationsForOperation.isEmpty()) {
            activationsForOperation.add(new Activation(evaluatedTime, additionalGuards, activationKind, parameters, probability, parametersAsString, parameterPredicates));
        } else {
            switch (activationKind) {
                case MULTI:
                    activateMultiOperations(activationsForOperation, new Activation(evaluatedTime, additionalGuards, activationKind, parameters, probability, parametersAsString, parameterPredicates));
                    break;
                case SINGLE_MIN: {
                    Activation activationForOperation = activationsForOperation.get(0);
                    int otherActivationTime = activationForOperation.getTime();
                    if (evaluatedTime < otherActivationTime) {
                        activationsForOperation.clear();
                        activationsForOperation.add(new Activation(evaluatedTime, additionalGuards, activationKind, parameters, probability, parametersAsString, parameterPredicates));
                    }
                    break;
                }
                case SINGLE_MAX: {
                    Activation activationForOperation = activationsForOperation.get(0);
                    int otherActivationTime = activationForOperation.getTime();
                    if (evaluatedTime > otherActivationTime) {
                        activationsForOperation.clear();
                        activationsForOperation.add(new Activation(evaluatedTime, additionalGuards, activationKind, parameters, probability, parametersAsString, parameterPredicates));
                    }
                    break;
                }
                case SINGLE:
                default:
                    break;
            }
        }
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
