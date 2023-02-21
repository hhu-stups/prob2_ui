package de.prob2.ui.simulation.configuration;

import java.nio.file.Path;
import java.util.List;

public class SimulationBlackBoxModelConfiguration implements ISimulationModelConfiguration {

	private List<Path> timedTraces;

	public SimulationBlackBoxModelConfiguration(List<Path> timedTraces) {
		this.timedTraces = timedTraces;
	}

	public List<Path> getTimedTraces() {
		return timedTraces;
	}

	public void setTimedTraces(List<Path> timedTraces) {
		this.timedTraces = timedTraces;
	}
}
