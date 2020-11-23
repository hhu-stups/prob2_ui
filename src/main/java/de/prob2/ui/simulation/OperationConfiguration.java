package de.prob2.ui.simulation;

public class OperationConfiguration {

    private String opName;

    private int time;

    private float probability;

    public OperationConfiguration(String opName, int time, float probability) {
        this.opName = opName;
        this.time = time;
        this.probability = probability;
    }

    public String getOpName() {
        return opName;
    }

    public int getTime() {
        return time;
    }

    public float getProbability() {
        return probability;
    }
}
