package de.prob2.ui.simulation.configuration;

import java.util.Map;

public class OperationConfiguration {

    private String opName;

    private int time;

    private Map<String, Integer> delay;

    private String probability;

    private int priority;

    private Map<String, Object> variableChoices;

    public OperationConfiguration(String opName, int time, Map<String, Integer> delay, String probability, int priority, Map<String, Object> variableChoices) {
        this.opName = opName;
        this.time = time;
        this.delay = delay;
        this.probability = probability;
        this.priority = priority;
        this.variableChoices = variableChoices;
    }

    public String getOpName() {
        return opName;
    }

    public int getTime() {
        return time;
    }

    public Map<String, Integer> getDelay() {
        return delay;
    }

    public String getProbability() {
        return probability;
    }

    public int getPriority() {
        return priority;
    }

    public Map<String, Object> getVariableChoices() {
        return variableChoices;
    }
}
