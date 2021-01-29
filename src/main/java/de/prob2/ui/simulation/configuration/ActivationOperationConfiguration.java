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
        StringBuilder sb = new StringBuilder();
        sb.append("ActivationOperationConfiguration(");
        sb.append("opName");
        sb.append("=");
        sb.append(opName);
        sb.append(", ");
        sb.append("time");
        sb.append("=");
        sb.append(time);
        sb.append(", ");
        if(additionalGuards != null) {
            sb.append("additionalGuards");
            sb.append("=");
            sb.append(additionalGuards);
            sb.append(", ");
        }
        sb.append("activationKind");
        sb.append("=");
        sb.append(activationKind);

        if(parameters != null) {
            sb.append(", ");
            sb.append("parameters");
            sb.append("=");
            sb.append(parameters);
            sb.append(", ");
        }
        if(probability != null) {
            sb.append(", ");
            sb.append("probability");
            sb.append("=");
            sb.append(probability);
        }
        sb.append(")");
        return sb.toString();
    }
}