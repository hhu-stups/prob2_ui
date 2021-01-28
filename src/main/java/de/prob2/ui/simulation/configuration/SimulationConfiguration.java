package de.prob2.ui.simulation.configuration;

import java.util.List;
import java.util.Map;

public class SimulationConfiguration {

    private final List<TimingConfiguration> timingConfigurations;

    public SimulationConfiguration(List<TimingConfiguration> timingConfigurations) {
        this.timingConfigurations = timingConfigurations;
    }

    public List<TimingConfiguration> getTimingConfigurations() {
        return timingConfigurations;
    }

}
