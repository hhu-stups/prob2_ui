package de.prob2.ui.simulation.configuration;


import java.util.List;
import java.util.Map;

public class OperationConfiguration {

    private String opName;

    private List<ActivationConfiguration> activation;

    private int priority;

    private Map<String, String> destState;

    public OperationConfiguration(String opName, List<ActivationConfiguration> activation, int priority, Map<String, String> destState) {
        this.opName = opName;
        this.activation = activation;
        this.priority = priority;
        this.destState = destState;
    }

    public String getOpName() {
        return opName;
    }

    public List<ActivationConfiguration> getActivation() {
        return activation;
    }

    public int getPriority() {
        return priority;
    }

    public Map<String, String> getDestState() {
        return destState;
    }
}
