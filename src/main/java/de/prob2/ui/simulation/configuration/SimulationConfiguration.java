package de.prob2.ui.simulation.configuration;

import java.util.List;

public class SimulationConfiguration {

	private int endingTime;

	private String startingCondition;

	private String endingCondition;

    private List<VariableChoice> setupConfigurations;

    private List<VariableChoice> initialisationConfigurations;

    private List<OperationConfiguration> operationConfigurations;

    public SimulationConfiguration(int endingTime, String startingCondition, String endingCondition, List<VariableChoice> setupConfigurations, List<VariableChoice> initialisationConfigurations, List<OperationConfiguration> operationConfigurations) {
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
