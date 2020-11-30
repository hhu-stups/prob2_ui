package de.prob2.ui.simulation;

import java.util.List;

public class OperationConfiguration {

    private String opName;

    private int time;

    private String probability;

    private int priority;

    private List<VariableChoice> variableChoices;

    public OperationConfiguration(String opName, int time, String probability, int priority, List<VariableChoice> variableChoices) {
        this.opName = opName;
        this.time = time;
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

    public String getProbability() {
        return probability;
    }

    public int getPriority() {
        return priority;
    }

    public List<VariableChoice> getVariableChoices() {
        return variableChoices;
    }
}
