package de.prob2.ui.simulation.simulators;

import java.util.Map;
import java.util.Objects;

public class Activation {

    private int time;

    private Map<String, String> parameters;

    private Object probability;

    private String firingTransitionParametersPredicate;

    public Activation(int time, Map<String, String> parameters, Object probability, String firingTransitionParametersPredicate) {
        this.time = time;
        this.parameters = parameters;
        this.probability = probability;
        this.firingTransitionParametersPredicate = firingTransitionParametersPredicate;
    }

    public void decreaseTime(int delta) {
        this.time -= delta;
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

    public String getFiringTransitionParametersPredicate() {
        return firingTransitionParametersPredicate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Activation that = (Activation) o;
        return time == that.time && Objects.equals(probability, that.probability) && Objects.equals(firingTransitionParametersPredicate, that.firingTransitionParametersPredicate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(time, probability, firingTransitionParametersPredicate);
    }

    @Override
    public String toString() {
        return String.format("Activation{time = %s, probability = %s, firingTransitionParametersPredicate = %s}", time, probability, firingTransitionParametersPredicate);
    }
}
