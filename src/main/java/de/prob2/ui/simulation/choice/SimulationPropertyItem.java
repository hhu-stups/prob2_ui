package de.prob2.ui.simulation.choice;

import de.prob2.ui.simulation.simulators.check.SimulationHypothesisChecker;
import javafx.beans.NamedArg;

public class SimulationPropertyItem {

	private SimulationCheckingType checkingType;

	public SimulationPropertyItem(@NamedArg("checkingType") SimulationCheckingType checkingType) {
		this.checkingType = checkingType;
	}

	@Override
	public String toString() {
		return checkingType.getName();
	}

	public SimulationCheckingType getCheckingType() {
		return checkingType;
	}

}
