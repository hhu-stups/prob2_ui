package de.prob2.ui.simulation.configuration;


import java.util.List;
import java.util.Map;

public class OperationConfiguration {

    private final String opName;

    private final List<ActivationConfiguration> activation;

    private final int priority;

    private final Map<String, String> destState;

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
