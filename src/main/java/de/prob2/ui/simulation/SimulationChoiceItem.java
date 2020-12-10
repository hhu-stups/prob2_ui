package de.prob2.ui.simulation;

import javafx.beans.NamedArg;

public class SimulationChoiceItem {

	private SimulationType simulationType;

	public SimulationChoiceItem(@NamedArg("simulationType") SimulationType simulationType) {
		this.simulationType = simulationType;
	}
	
	@Override
	public String toString() {
		return simulationType.getName();
	}
	
	public SimulationType getSimulationType() {
		return simulationType;
	}

}