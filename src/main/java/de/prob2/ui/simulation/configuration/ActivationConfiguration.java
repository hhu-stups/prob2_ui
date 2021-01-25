package de.prob2.ui.simulation.configuration;

public class ActivationConfiguration {

    private int time;

    private Object probability;

    public ActivationConfiguration(int time, Object probability) {
        this.time = time;
        this.probability = probability;
    }

    public int getTime() {
        return time;
    }

    public Object getProbability() {
        return probability;
    }

}
