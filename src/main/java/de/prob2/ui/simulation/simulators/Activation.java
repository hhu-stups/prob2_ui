package de.prob2.ui.simulation.simulators;

import de.prob2.ui.simulation.configuration.ActivationOperationConfiguration;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Activation {

    private int time;

    private final String additionalGuards;

    private final ActivationOperationConfiguration.ActivationKind activationKind;

    private final Map<String, String> fixedVariables;

    private final Object probabilisticVariables;

    private final List<String> firingTransitionParameters;

    private final String firingTransitionParametersPredicate;

    public Activation(int time, String additionalGuards, ActivationOperationConfiguration.ActivationKind activationKind,
                      Map<String, String> fixedVariables, Object probabilisticVariables, List<String> firingTransitionParameters, String firingTransitionParametersPredicate) {
        this.time = time;
        this.additionalGuards = additionalGuards;
        this.activationKind = activationKind;
        this.fixedVariables = fixedVariables;
        this.probabilisticVariables = probabilisticVariables;
        this.firingTransitionParameters = firingTransitionParameters;
        this.firingTransitionParametersPredicate = firingTransitionParametersPredicate;
    }

    public void decreaseTime(int delta) {
        this.time -= delta;
    }

    public int getTime() {
        return time;
    }

    public String getAdditionalGuards() {
        return additionalGuards;
    }

    public ActivationOperationConfiguration.ActivationKind getActivationKind() {
        return activationKind;
    }

    public Map<String, String> getFixedVariables() {
        return fixedVariables;
    }

    public Object getProbabilisticVariables() {
        return probabilisticVariables;
    }

    public List<String> getFiringTransitionParameters() {
        return firingTransitionParameters;
    }

    public String getFiringTransitionParametersPredicate() {
        return firingTransitionParametersPredicate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Activation that = (Activation) o;
        return time == that.time && Objects.equals(additionalGuards, that.additionalGuards) && activationKind == that.activationKind && Objects.equals(probabilisticVariables, that.probabilisticVariables) && Objects.equals(firingTransitionParameters, that.firingTransitionParameters) && Objects.equals(firingTransitionParametersPredicate, that.firingTransitionParametersPredicate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(time, additionalGuards, activationKind, probabilisticVariables, firingTransitionParameters, firingTransitionParametersPredicate);
    }

    @Override
    public String toString() {
        return String.format("Activation{time = %s, probability = %s, additionalGuards = %s, activationKind = %s, firingTransitionParameters = %s, firingTransitionParametersPredicate = %s}", time, probabilisticVariables, additionalGuards, activationKind, firingTransitionParameters, firingTransitionParametersPredicate);
    }
}
