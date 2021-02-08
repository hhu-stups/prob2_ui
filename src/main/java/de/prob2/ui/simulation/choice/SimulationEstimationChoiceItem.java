package de.prob2.ui.simulation.choice;

import de.prob2.ui.simulation.simulators.check.SimulationEstimator;
import de.prob2.ui.simulation.simulators.check.SimulationHypothesisChecker;
import javafx.beans.NamedArg;

public class SimulationEstimationChoiceItem {

	private SimulationEstimator.EstimationType estimationType;

	public SimulationEstimationChoiceItem(@NamedArg("estimationType") SimulationEstimator.EstimationType estimationType) {
		this.estimationType = estimationType;
	}

	@Override
	public String toString() {
		return estimationType.getName();
	}

	public SimulationEstimator.EstimationType getEstimationType() {
		return estimationType;
	}

}
