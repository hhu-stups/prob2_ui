package de.prob2.ui.simulation.configuration;

import java.util.List;
import java.util.Map;

public class OperationConfiguration {

    public static enum ActivationKind {
        SINGLE, SINGLE_MIN, SINGLE_MAX, MULTI
    }

    private List<String> opName;

    private List<Map<String, Integer>> activation;

    private ActivationKind activationKind;

    private List<String> probability;

    private int priority;

    private List<Map<String, Object>> variableChoices;

    public OperationConfiguration(List<String> opName, List<Map<String, Integer>> activation, ActivationKind activationKind, List<String> probability,
                                  int priority, List<Map<String, Object>> variableChoices) {
        this.opName = opName;
        this.activation = activation;
        this.activationKind = activationKind;
        this.probability = probability;
        this.priority = priority;
        this.variableChoices = variableChoices;
    }

    public List<String> getOpName() {
        return opName;
    }

    public List<Map<String, Integer>> getActivation() {
        return activation;
    }

    public ActivationKind getActivationKind() {
        return activationKind;
    }

    public List<String> getProbability() {
        return probability;
    }

    public int getPriority() {
        return priority;
    }

    public List<Map<String, Object>> getVariableChoices() {
        return variableChoices;
    }
}
