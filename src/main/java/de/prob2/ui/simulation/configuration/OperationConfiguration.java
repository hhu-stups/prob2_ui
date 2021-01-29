package de.prob2.ui.simulation.configuration;


import java.util.List;
import java.util.Map;

public class OperationConfiguration {

    public enum ActivationKind {
        SINGLE, SINGLE_MIN, SINGLE_MAX, MULTI
    }

    private String opName;

    private List<ActivationConfiguration> activation;

    private ActivationKind activationKind;

    private String additionalGuards;

    private int priority;

    private Map<String, String> destState;

    public OperationConfiguration(String opName, List<ActivationConfiguration> activation, ActivationKind activationKind,
                                  String additionalGuards, int priority, Map<String, String> destState) {
        this.opName = opName;
        this.activation = activation;
        this.activationKind = activationKind;
        this.additionalGuards = additionalGuards;
        this.priority = priority;
        this.destState = destState;
    }

    public String getOpName() {
        return opName;
    }

    public List<ActivationConfiguration> getActivation() {
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

    public Map<String, String> getDestState() {
        return destState;
    }
}
