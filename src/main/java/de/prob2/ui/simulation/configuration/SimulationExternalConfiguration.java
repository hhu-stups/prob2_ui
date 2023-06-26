package de.prob2.ui.simulation.configuration;

import java.nio.file.Path;

public class SimulationExternalConfiguration implements ISimulationModelConfiguration {

	private Path externalPath;

	public SimulationExternalConfiguration(Path externalPath) {
		this.externalPath = externalPath;
	}

	public Path getExternalPath() {
		return externalPath;
	}

	public void setExternalPath(Path externalPath) {
		this.externalPath = externalPath;
	}
}
