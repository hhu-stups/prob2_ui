package de.prob2.ui.simulation.configuration;

import java.util.Map;

public class ActivationConfiguration {

    private int time;

    private Map<String, String> parameters;

    private Object probability;

    public ActivationConfiguration(int time, Map<String, String> parameters, Object probability) {
        this.time = time;
        this.probability = probability;
    }

    public int getTime() {
        return time;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public Object getProbability() {
        return probability;
    }

}