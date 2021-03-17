package de.prob2.ui.simulation.simulators.check;

import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob.statespace.Trace;
import de.prob2.ui.simulation.choice.SimulationCheckingType;

import java.util.Map;

public class SimulationEstimator extends AbstractSimulationMonteCarlo {

    public enum EstimationType {
        MEAN("Mean estimator");

        private String name;

        EstimationType(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    private final EstimationType estimationType;

    private final double desiredValue;

    private final double epsilon;

    public SimulationEstimator(final CurrentTrace currentTrace, Trace trace, int numberExecutions, SimulationCheckingType type,
                               final EstimationType estimationType, final double desiredValue, final double epsilon, Map<String, Object> additionalInformation) {
        super(currentTrace, trace, numberExecutions, type, additionalInformation);
        this.estimationType = estimationType;
        this.desiredValue = desiredValue;
        this.epsilon = epsilon;
    }

    private void checkMean() {
        int n = resultingTraces.size();
        double ratio = (double) numberSuccess / n;
        if(ratio >= desiredValue - epsilon && ratio <= desiredValue + epsilon) {
            this.result = MonteCarloCheckResult.SUCCESS;
        } else {
            this.result = MonteCarloCheckResult.FAIL;
        }
    }

    public void check() {
        switch (estimationType) {
            case MEAN:
                checkMean();
                break;
            default:
                // TODO: New estimation types
                break;
        }

    }

    @Override
    protected void calculateStatistics() {
        int n = resultingTraces.size();
        double ratio = (double) numberSuccess / n;
        this.stats = new SimulationStats(n, numberSuccess, ratio, calculateExtendedStats());
    }

}
