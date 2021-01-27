package de.prob2.ui.simulation.configuration;

import java.util.Map;

public class ActivationConfiguration {

    private String time;

    private Map<String, String> parameters;

    private Object probability;

    public ActivationConfiguration(String time, Map<String, String> parameters, Object probability) {
        this.time = time;
        this.parameters = parameters;
        this.probability = probability;
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
        return String.format("ActivationConfiguration{time=%s, parameters=%s, probability=%s}", time, parameters, probability);
    }
}