package de.prob2.ui.simulation.configuration;

import java.util.List;
import java.util.Map;

public class SimulationConfiguration {

    private Map<String, Object> setupConfigurations;

    private Map<String, Object> initialisationConfigurations;

    private List<OperationConfiguration> operationConfigurations;

    public SimulationConfiguration(Map<String, Object> setupConfigurations, Map<String, Object> initialisationConfigurations, List<OperationConfiguration> operationConfigurations) {
    	this.setupConfigurations = setupConfigurations;
        this.initialisationConfigurations = initialisationConfigurations;
        this.operationConfigurations = operationConfigurations;
    }


	public Map<String, Object> getSetupConfigurations() {
        return setupConfigurations;
    }

    public Map<String, Object> getInitialisationConfigurations() {
        return initialisationConfigurations;
    }

    public List<OperationConfiguration> getOperationConfigurations() {
        return operationConfigurations;
    }
}
