package de.prob2.ui.simulation.table;

import de.prob2.ui.simulation.configuration.ActivationOperationConfiguration;

import java.util.List;
import java.util.Map;

public class SimulationOperationDebugItem extends SimulationDebugItem {

    private final String opName;

    private final String time;

    private final String priority;

    private final List<String> activations;

    private final ActivationOperationConfiguration.ActivationKind activationKind;

    private final String additionalGuards;

    private final Map<String, String> fixedVariables;

    private final Object probabilisticVariables;

    public SimulationOperationDebugItem(String id, String opName, String time, String priority, List<String> activations, ActivationOperationConfiguration.ActivationKind activationKind,
                                        String additionalGuards, Map<String, String> fixedVariables, Object probabilisticVariables) {
        super(id);
        this.opName = opName;
        this.time = time;
        this.priority = priority;
        this.activations = activations;
        this.activationKind = activationKind;
        this.additionalGuards = additionalGuards;
        this.fixedVariables = fixedVariables;
        this.probabilisticVariables = probabilisticVariables;
    }

    public String getOpName() {
        return opName;
    }

    public String getTime() {
        return time;
    }

    public String getPriority() {
        return priority;
    }

    public List<String> getActivations() {
        return activations;
    }

    public String getActivationsAsString() {
        return activations == null ? "" : activations.toString();
    }

    public ActivationOperationConfiguration.ActivationKind getActivationKind() {
        return activationKind;
    }

    public String getAdditionalGuards() {
        return additionalGuards;
    }

    public String getAdditionalGuardsAsString() {
        return additionalGuards == null ? "" : additionalGuards;
    }

    public Map<String, String> getFixedVariables() {
        return fixedVariables;
    }

    public String getFixedVariablesAsString() {
        return fixedVariables == null ? "" : fixedVariables.toString();
    }

    public Object getProbabilisticVariables() {
        return probabilisticVariables;
    }

    public String getProbabilisticVariablesAsString() {
        return probabilisticVariables == null ? "" : probabilisticVariables.toString();
    }
}
