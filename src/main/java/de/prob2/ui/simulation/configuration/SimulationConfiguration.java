package de.prob2.ui.simulation.configuration;

import java.util.List;
import java.util.Map;

public class SimulationConfiguration {

	private int endingTime;

	private String startingCondition;

	private String endingCondition;

    private Map<String, Object> setupConfigurations;

    private Map<String, Object> initialisationConfigurations;

    private List<OperationConfiguration> operationConfigurations;

    public SimulationConfiguration(int endingTime, String startingCondition, String endingCondition, Map<String, Object> setupConfigurations, Map<String, Object> initialisationConfigurations, List<OperationConfiguration> operationConfigurations) {
        this.endingTime = endingTime;
        this.startingCondition = startingCondition;
        this.endingCondition = endingCondition;
    	this.setupConfigurations = setupConfigurations;
        this.initialisationConfigurations = initialisationConfigurations;
        this.operationConfigurations = operationConfigurations;
    }

	public int getEndingTime() {
		return endingTime;
	}

	public String getStartingCondition() {
		return startingCondition;
	}

	public String getEndingCondition() {
		return endingCondition;
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
