package de.prob2.ui.simulation.configuration;

import java.util.List;
import java.util.Map;

public class OperationConfiguration {

    private List<String> opName;

    private int time;

    private List<Map<String, Integer>> delay;

    private List<String> probability;

    private int priority;

    private List<Map<String, Object>> variableChoices;

    public OperationConfiguration(List<String> opName, int time, List<Map<String, Integer>> delay, List<String> probability,
                                  int priority, List<Map<String, Object>> variableChoices) {
        this.opName = opName;
        this.time = time;
        this.delay = delay;
        this.probability = probability;
        this.priority = priority;
        this.variableChoices = variableChoices;
    }

    public List<String> getOpName() {
        return opName;
    }

    public int getTime() {
        return time;
    }

    public List<Map<String, Integer>> getDelay() {
        return delay;
    }

    public List<String> getProbability() {
        return probability;
    }

    public int getPriority() {
        return priority;
    }

    public List<Map<String, Object>> getVariableChoices() {
        return variableChoices;
    }
}
