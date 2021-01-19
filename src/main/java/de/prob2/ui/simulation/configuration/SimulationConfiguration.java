package de.prob2.ui.simulation.configuration;

import java.util.List;
import java.util.Map;

public class SimulationConfiguration {

    private List<OperationConfiguration> operationConfigurations;

    public SimulationConfiguration(List<OperationConfiguration> operationConfigurations) {
        this.operationConfigurations = operationConfigurations;
    }

    public List<OperationConfiguration> getOperationConfigurations() {
        return operationConfigurations;
    }
}
