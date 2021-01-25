package de.prob2.ui.simulation.configuration;

import java.util.List;
import java.util.Map;

public class SimulationConfiguration {

    private Map<String, ActivationConfiguration> activationConfigurations;

    private List<TimingConfiguration> timingConfigurations;

    public SimulationConfiguration(Map<String, ActivationConfiguration> activationConfigurations, List<TimingConfiguration> timingConfigurations) {
        this.activationConfigurations = activationConfigurations;
        this.timingConfigurations = timingConfigurations;
    }

    public Map<String, ActivationConfiguration> getActivationConfigurations() {
        return activationConfigurations;
    }

    public List<TimingConfiguration> getTimingConfigurations() {
        return timingConfigurations;
    }

}
