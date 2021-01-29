package de.prob2.ui.simulation.configuration;

import java.util.Map;

public class ActivationOperationConfiguration extends ActivationConfiguration {

    public enum ActivationKind {
        SINGLE, SINGLE_MIN, SINGLE_MAX, MULTI
    }

    private final String opName;

    private final String time;

    private final String additionalGuards;

    private final ActivationKind activationKind;

    private final Map<String, String> parameters;

    private final Object probability;

    public ActivationOperationConfiguration(String opName, String time, String additionalGuards, ActivationKind activationKind,
                                            Map<String, String> parameters, Object probability) {
        super();
        this.opName = opName;
        this.time = time;
        this.additionalGuards = additionalGuards;
        this.activationKind = activationKind;
        this.parameters = parameters;
        this.probability = probability;
    }

    public String getOpName() {
        return opName;
    }

    public String getTime() {
        return time;
    }

    public String getAdditionalGuards() {
        return additionalGuards;
    }

    public ActivationKind getActivationKind() {
        return activationKind;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public Object getProbability() {
        return probability;
    }

    @Override
    public String toString() {
        return String.format("ActivationOperationConfiguration{opName=%s, time=%s, additionalGuards=%s, activationKind=%s, parameters=%s, probability=%s}", opName, time, additionalGuards, activationKind, parameters, probability);
    }
}