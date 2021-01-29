package de.prob2.ui.simulation.configuration;

import java.util.List;

public class ActivationChoiceConfiguration extends ActivationConfiguration {

    private List<ActivationConfiguration> activations;

    private List<String> probability;

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
        return String.format("ActivationChoiceActivation{activations=%s, probability=%s}", activations, probability);
    }
}
