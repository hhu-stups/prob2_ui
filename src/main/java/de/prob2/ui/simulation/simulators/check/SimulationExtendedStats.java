package de.prob2.ui.simulation.simulators.check;

import java.util.Map;

public class SimulationExtendedStats {

    private final Map<String, Integer> operationExecutions;

    private final Map<String, Integer> operationEnablings;

    private final Map<String, Double> operationPercentage;

    public SimulationExtendedStats(Map<String, Integer> operationExecutions, Map<String, Integer> operationEnablings,
                                   Map<String, Double> operationPercentage) {
        this.operationExecutions = operationExecutions;
        this.operationEnablings = operationEnablings;
        this.operationPercentage = operationPercentage;
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
