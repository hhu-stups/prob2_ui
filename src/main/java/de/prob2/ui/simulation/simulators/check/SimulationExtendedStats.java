package de.prob2.ui.simulation.simulators.check;

import java.util.Map;

public class SimulationExtendedStats {

    private final int lengthTraces;

    private final Map<String, Integer> operationExecutions;

    private final Map<String, Integer> operationEnablings;

    private final Map<String, Double> operationPercentage;

    public SimulationExtendedStats(int lengthTraces, Map<String, Integer> operationExecutions, Map<String, Integer> operationEnablings,
                                   Map<String, Double> operationPercentage) {
        this.lengthTraces = lengthTraces;
        this.operationExecutions = operationExecutions;
        this.operationEnablings = operationEnablings;
        this.operationPercentage = operationPercentage;
    }

    public int getLengthTraces() {
        return lengthTraces;
    }

    public Map<String, Integer> getOperationExecutions() {
        return operationExecutions;
    }

    public Map<String, Integer> getOperationEnablings() {
        return operationEnablings;
    }

    public Map<String, Double> getOperationPercentage() {
        return operationPercentage;
    }
}
