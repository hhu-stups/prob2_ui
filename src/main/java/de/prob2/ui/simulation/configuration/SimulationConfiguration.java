package de.prob2.ui.simulation.configuration;

import java.util.List;

public class SimulationConfiguration {

    private final List<ActivationConfiguration> activationConfigurations;

    public SimulationConfiguration(List<ActivationConfiguration> activationConfigurations) {
        this.activationConfigurations = activationConfigurations;
    }

    public List<ActivationConfiguration> getActivationConfigurations() {
        return activationConfigurations;
    }

}
