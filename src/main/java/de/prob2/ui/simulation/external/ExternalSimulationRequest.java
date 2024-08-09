package de.prob2.ui.simulation.external;

public class ExternalSimulationRequest {

	private int finished;

	private String enabledOperations;

	public ExternalSimulationRequest(int finished, String enabledOperations) {
		this.finished = finished;
		this.enabledOperations = enabledOperations;
	}

	public int getFinished() {
		return finished;
	}

	public String getEnabledOperations() {
		return enabledOperations;
	}

}
