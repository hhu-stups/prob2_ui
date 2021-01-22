package de.prob2.ui.simulation.configuration;

import java.util.List;

public class SimulationConfiguration {

    private List<TimingConfiguration> timingConfigurations;

    private List<ProbabilisticConfiguration> probabilisticConfigurations;

    public SimulationConfiguration(List<TimingConfiguration> timingConfigurations, List<ProbabilisticConfiguration> probabilisticConfigurations) {
        this.timingConfigurations = timingConfigurations;
        this.probabilisticConfigurations = probabilisticConfigurations;
    }

    public List<TimingConfiguration> getTimingConfigurations() {
        return timingConfigurations;
    }

    public List<ProbabilisticConfiguration> getProbabilisticConfigurations() {
        return probabilisticConfigurations;
    }
}
