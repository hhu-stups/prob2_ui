package de.prob2.ui.simulation;

import java.util.List;

public class SimulationConfiguration {

    private int time;

    private List<OperationConfiguration> operationConfigurations;

    public SimulationConfiguration(int time, List<OperationConfiguration> operationConfigurations) {
        this.time = time;
        this.operationConfigurations = operationConfigurations;
    }

    public int getTime() {
        return time;
    }

    public List<OperationConfiguration> getOperationConfigurations() {
        return operationConfigurations;
    }
}
