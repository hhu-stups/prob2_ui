package de.prob2.ui.simulation.configuration;

import java.util.List;

public class ActivationChoiceConfiguration extends ActivationConfiguration {

    private final List<ActivationConfiguration> activations;

    private final List<String> probability;

    public ActivationChoiceConfiguration(List<ActivationConfiguration> activations, List<String> probability) {
        super();
        this.activations = activations;
        this.probability = probability;
    }

    public List<ActivationConfiguration> getActivations() {
        return activations;
    }

    public List<String> getProbability() {
        return probability;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ActivationChoiceActivation(");
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
