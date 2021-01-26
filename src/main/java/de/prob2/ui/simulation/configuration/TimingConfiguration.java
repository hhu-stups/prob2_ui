package de.prob2.ui.simulation.configuration;


import java.util.List;
import java.util.Map;

public class TimingConfiguration {

    public enum ActivationKind {
        SINGLE, SINGLE_MIN, SINGLE_MAX, MULTI
    }

    private String opName;

    private Map<String, List<ActivationConfiguration>> activation;

    private ActivationKind activationKind;

    private String additionalGuards;

    private int priority;

    private Map<String, String> variableChoices;

    public TimingConfiguration(String opName, Map<String, List<ActivationConfiguration>> activation, ActivationKind activationKind,
                               String additionalGuards, int priority, Map<String, String> variableChoices) {
        this.opName = opName;
        this.activation = activation;
        this.activationKind = activationKind;
        this.additionalGuards = additionalGuards;
        this.priority = priority;
        this.variableChoices = variableChoices;
    }

    public String getOpName() {
        return opName;
    }

    public Map<String, List<ActivationConfiguration>> getActivation() {
        return activation;
    }

    public ActivationKind getActivationKind() {
        return activationKind;
    }

    public String getAdditionalGuards() {
        return additionalGuards;
    }

    public int getPriority() {
        return priority;
    }

    public Map<String, String> getVariableChoices() {
        return variableChoices;
    }
}
