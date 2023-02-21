package de.prob2.ui.simulation.configuration;

import java.util.List;

public class SimulationBlackBoxModelConfiguration implements ISimulationModel {

	private List<SimulationModelConfiguration> timedTraces;

	public SimulationBlackBoxModelConfiguration(List<SimulationModelConfiguration> timedTraces) {
		this.timedTraces = timedTraces;
	}

	public List<SimulationModelConfiguration> getTimedTraces() {
		return timedTraces;
	}

	public void setTimedTraces(List<SimulationModelConfiguration> timedTraces) {
		this.timedTraces = timedTraces;
	}
}
