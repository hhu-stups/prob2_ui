package de.prob2.ui.simulation.configuration;

import java.io.Serial;

public class ConfigurationCheckingError extends Exception {
	@Serial
	private static final long serialVersionUID = 1L;

	public ConfigurationCheckingError(String msg) {
		super(msg);
	}

}
