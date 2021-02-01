package de.prob2.ui.simulation.configuration;

import java.util.List;
import java.util.Map;

public class ActivationOperationConfiguration extends ActivationConfiguration {

    public enum ActivationKind {
        SINGLE, SINGLE_MIN, SINGLE_MAX, MULTI
    }

    private final String opName;

    private final String time;

    private final int priority;

    private final String additionalGuards;

    private final ActivationKind activationKind;

    private final Map<String, String> parameters;

    private final Object probability;

    private final List<String> activation;

    public ActivationOperationConfiguration(String id, String opName, String time, int priority, String additionalGuards, ActivationKind activationKind,
                                            Map<String, String> parameters, Object probability, List<String> activation) {
        super(id);
        this.opName = opName;
        this.time = time;
        this.priority = priority;
        this.additionalGuards = additionalGuards;
        this.activationKind = activationKind;
        this.parameters = parameters;
        this.probability = probability;
        this.activation = activation;
    }

    public String getOpName() {
        return opName;
    }

    public String getTime() {
        return time;
    }

    public int getPriority() {
        return priority;
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

    public List<String> getActivation() {
        return activation;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ActivationOperationConfiguration(");
        sb.append("id");
        sb.append("=");
        sb.append(id);
        sb.append(", ");
        sb.append("opName");
        sb.append("=");
        sb.append(opName);
        sb.append(", ");
        sb.append("time");
        sb.append("=");
        sb.append(time);
        sb.append(", ");
        sb.append("priority");
        sb.append("=");
        sb.append(priority);
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
            sb.append(", ");
        }
        if(activation != null) {
            sb.append(", ");
            sb.append("activation");
            sb.append("=");
            sb.append(activation);
        }
        sb.append(")");
        return sb.toString();
    }
}