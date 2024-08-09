package de.prob2.ui.simulation.external;

import java.io.Serial;

public class ExternalSimulationRuntimeException extends RuntimeException {
	@Serial
	private static final long serialVersionUID = 1L;

	private String message;

	public ExternalSimulationRuntimeException(){}

	public ExternalSimulationRuntimeException(String message){
		this.message = message;
	}

	@Override
	public String getMessage(){
		return message;
	}
}
