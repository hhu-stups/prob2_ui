package de.prob2.ui.simulation;

import de.prob2.ui.simulation.simulators.check.SimulationHypothesisChecker;
import javafx.beans.NamedArg;

public class SimulationPropertyItem {

	private SimulationHypothesisChecker.CheckingType checkingType;

	public SimulationPropertyItem(@NamedArg("checkingType") SimulationHypothesisChecker.CheckingType checkingType) {
		this.checkingType = checkingType;
	}

	@Override
	public String toString() {
		return checkingType.getName();
	}

	public SimulationHypothesisChecker.CheckingType getCheckingType() {
		return checkingType;
	}

}
