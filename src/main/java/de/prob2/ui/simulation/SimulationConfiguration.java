package de.prob2.ui.simulation;

import java.util.List;

public class SimulationConfiguration {

    private List<VariableChoice> setupConfigurations;

    private List<VariableChoice> initialisationConfigurations;

    private List<OperationConfiguration> operationConfigurations;

    public SimulationConfiguration(List<VariableChoice> setupConfigurations, List<VariableChoice> initialisationConfigurations, List<OperationConfiguration> operationConfigurations) {
        this.setupConfigurations = setupConfigurations;
        this.initialisationConfigurations = initialisationConfigurations;
        this.operationConfigurations = operationConfigurations;
    }

    public List<VariableChoice> getSetupConfigurations() {
        return setupConfigurations;
    }

    public List<VariableChoice> getInitialisationConfigurations() {
        return initialisationConfigurations;
    }

    public List<OperationConfiguration> getOperationConfigurations() {
        return operationConfigurations;
    }
}
