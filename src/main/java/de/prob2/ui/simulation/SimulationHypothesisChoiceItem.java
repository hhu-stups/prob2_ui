package de.prob2.ui.simulation;

import de.prob2.ui.simulation.simulators.check.SimulationHypothesisChecker;
import javafx.beans.NamedArg;

public class SimulationHypothesisChoiceItem {

	private SimulationHypothesisChecker.HypothesisCheckingType checkingType;

	public SimulationHypothesisChoiceItem(@NamedArg("checkingType") SimulationHypothesisChecker.HypothesisCheckingType checkingType) {
		this.checkingType = checkingType;
	}

	@Override
	public String toString() {
		return checkingType.getName();
	}

	public SimulationHypothesisChecker.HypothesisCheckingType getCheckingType() {
		return checkingType;
	}

}
