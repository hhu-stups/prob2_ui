package de.prob2.ui.visualisation.fx.exception;

import java.io.Serial;

/**
 * Exception that gets thrown when a formula of a visualisation could not be parsed.
 */
public class VisualisationParseException extends Exception{
	@Serial
	private static final long serialVersionUID = 1L;

	private final String formula;

	public VisualisationParseException(String formula, Throwable cause) {
		super(cause);
		this.formula = formula;
	}

	public String getFormula() {
		return formula;
	}
}
