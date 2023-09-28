package de.prob2.ui.simulation;

import java.io.Serial;

public class SimulationError extends RuntimeException {
	@Serial
	private static final long serialVersionUID = 1L;

	private String message;

	public SimulationError(){}

	public SimulationError(String message){
		this.message = message;
	}

	@Override
	public String getMessage(){
		return message;
	}
}
