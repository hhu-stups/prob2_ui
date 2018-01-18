package de.prob2.ui.visualisation.fx.exception;

/**
 * Exception that gets thrown when a formula of a visualisation could not be parsed.
 *
 * @author Christoph Heinzen
 * @version 0.1.0
 * @since 18.01.18
 */
public class VisualisationParseException extends Exception{

	private final String formula;

	public VisualisationParseException(String formula, Throwable cause) {
		super(cause);
		this.formula = formula;
	}

	public String getFormula() {
		return formula;
	}
}
