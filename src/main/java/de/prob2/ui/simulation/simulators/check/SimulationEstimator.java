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

    public SimulationEstimator(final CurrentTrace currentTrace, Trace trace, int numberExecutions, SimulationCheckingType type, Map<String, Object> additionalInformation) {
        super(currentTrace, trace, numberExecutions, type, additionalInformation);
    }


    public void check() {
        // TODO do after run
        // TODO estimate
        // What do we want to estimate? Probability? Integer expressions? Boolean expressions? What are the conditions for estimation? Other complicated types e.g. expressions might be hard
    }

}
