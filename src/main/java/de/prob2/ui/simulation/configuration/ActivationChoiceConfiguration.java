package de.prob2.ui.simulation.configuration;

import java.util.List;

public class ActivationChoiceConfiguration extends ActivationConfiguration {

    private final List<String> activations;

    private final List<String> probability;

    public ActivationChoiceConfiguration(String id, List<String> activations, List<String> probability) {
        super(id);
        this.activations = activations;
        this.probability = probability;
    }

    public List<String> getActivations() {
        return activations;
    }

    public List<String> getProbability() {
        return probability;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ActivationChoiceActivation(");
        sb.append("id");
        sb.append("=");
        sb.append(id);
        sb.append(", ");
        sb.append("activations");
        sb.append("=");
        sb.append(activations);
        sb.append(", ");
        sb.append("probability");
        sb.append("=");
        sb.append(probability);
        sb.append(")");
        return sb.toString();
    }
}
