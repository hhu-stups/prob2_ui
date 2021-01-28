package de.prob2.ui.simulation.configuration;

import java.util.List;

public class SimulationConfiguration {

    private final List<OperationConfiguration> operationConfigurations;

    public SimulationConfiguration(List<OperationConfiguration> operationConfigurations) {
        this.operationConfigurations = operationConfigurations;
    }

    public List<OperationConfiguration> getOperationConfigurations() {
        return operationConfigurations;
    }

}
