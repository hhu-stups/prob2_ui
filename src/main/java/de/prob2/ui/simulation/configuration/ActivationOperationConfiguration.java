package de.prob2.ui.simulation.configuration;

import java.util.List;
import java.util.Map;

public class ActivationOperationConfiguration extends ActivationConfiguration {

    public enum ActivationKind {
        SINGLE, SINGLE_MIN, SINGLE_MAX, MULTI
    }

    private final String op;

    private final String time;

    private final int priority;

    private final String additionalGuards;

    private final ActivationKind activationKind;

    private final Map<String, String> fixedVariables;

    private final Object probabilisticVariables;

    private final List<String> activations;

    public ActivationOperationConfiguration(String id, String op, String time, int priority, String additionalGuards, ActivationKind activationKind,
                                            Map<String, String> fixedVariables, Object probabilisticVariables, List<String> activations) {
        super(id);
        this.op = op;
        this.time = time;
        this.priority = priority;
        this.additionalGuards = additionalGuards;
        this.activationKind = activationKind;
        this.fixedVariables = fixedVariables;
        this.probabilisticVariables = probabilisticVariables;
        this.activations = activations;
    }

    public String getOpName() {
        return op;
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

    public Map<String, String> getFixedVariables() {
        return fixedVariables;
    }

    public Object getProbabilisticVariables() {
        return probabilisticVariables;
    }

    public List<String> getActivations() {
        return activations;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ActivationOperationConfiguration(");
        sb.append("id");
        sb.append("=");
        sb.append(id);
        sb.append(", ");
        sb.append("op");
        sb.append("=");
        sb.append(op);
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

        if(fixedVariables != null) {
            sb.append(", ");
            sb.append("fixedVariables");
            sb.append("=");
            sb.append(fixedVariables);
            sb.append(", ");
        }
        if(probabilisticVariables != null) {
            sb.append(", ");
            sb.append("probabilisticVariables");
            sb.append("=");
            sb.append(probabilisticVariables);
            sb.append(", ");
        }
        if(activations != null) {
            sb.append(", ");
            sb.append("activations");
            sb.append("=");
            sb.append(activations);
        }
        sb.append(")");
        return sb.toString();
    }
}
