package de.prob2.ui.simulation.configuration;

import java.util.Map;

public class ActivationOperationConfiguration extends ActivationConfiguration {

    private String opName;

    private String time;

    private Map<String, String> parameters;

    private Object probability;

    public ActivationOperationConfiguration(String opName, String time, Map<String, String> parameters, Object probability) {
        super();
        this.opName = opName;
        this.time = time;
        this.parameters = parameters;
        this.probability = probability;
    }

    public String getOpName() {
        return opName;
    }

    public String getTime() {
        return time;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public Object getProbability() {
        return probability;
    }

    @Override
    public String toString() {
        return String.format("ActivationOperationConfiguration{opName=%s, time=%s, parameters=%s, probability=%s}", opName, time, parameters, probability);
    }
}