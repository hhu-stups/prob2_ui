package de.prob2.ui.simulation.simulators;

import de.prob2.ui.simulation.configuration.ActivationConfiguration;

import java.util.Objects;

public class Activation {

    private int time;

    private Object probability;

    public Activation(ActivationConfiguration activationConfiguration) {
        this.time = activationConfiguration.getTime();
        this.probability = activationConfiguration.getProbability();
    }

    public void decreaseTime(int delta) {
        this.time -= delta;
    }

    public int getTime() {
        return time;
    }

    public Object getProbability() {
        return probability;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Activation that = (Activation) o;
        return time == that.time && Objects.equals(probability, that.probability);
    }

    @Override
    public int hashCode() {
        return Objects.hash(time, probability);
    }

    @Override
    public String toString() {
        return String.format("Activation{time = %s, probability = %s}", time, probability);
    }
}
