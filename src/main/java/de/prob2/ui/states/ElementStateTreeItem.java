package de.prob2.ui.states;

import java.util.Map;

import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob.animator.domainobjects.EnumerationWarning;
import de.prob.animator.domainobjects.EvalResult;
import de.prob.animator.domainobjects.EvaluationErrorResult;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.animator.domainobjects.IdentifierNotInitialised;
import de.prob.animator.domainobjects.WDError;
import de.prob.model.representation.AbstractElement;
import de.prob.model.representation.AbstractFormulaElement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ElementStateTreeItem extends StateTreeItem<AbstractElement> {
	private static final Logger LOGGER = LoggerFactory.getLogger(ElementStateTreeItem.class);
	
	private final AbstractEvalResult result;
	private final AbstractEvalResult previousResult;
	
	public ElementStateTreeItem(final String name, final AbstractElement contents, final AbstractEvalResult result, final AbstractEvalResult previousResult) {
		super(name, stringRep(result), stringRep(previousResult), contents);
		
		this.result = result;
		this.previousResult = previousResult;
	}
	
	private static String stringRep(final AbstractEvalResult res) {
		if (res == null) {
			return "";
		} else if (res instanceof EvalResult) {
			return ((EvalResult) res).getValue();
		} else if (res instanceof IdentifierNotInitialised) {
			return "(not initialized)";
		} else if (res instanceof WDError) {
			return "(not well-defined)";
		} else if (res instanceof EvaluationErrorResult) {
			return "Error: " + ((EvaluationErrorResult) res).getResult();
		} else if (res instanceof EnumerationWarning) {
			return "(enumeration warning)";
		} else {
			LOGGER.warn("Unknown result type, falling back to toString: {}", res.getClass());
			// noinspection ObjectToString
			return res.getClass() + " toString: " + res;
		}
	}
	
	public static ElementStateTreeItem fromElementAndValues(
		final AbstractElement element,
		final Map<IEvalElement, AbstractEvalResult> values,
		final Map<IEvalElement, AbstractEvalResult> previousValues
	) {
		final AbstractEvalResult result;
		final AbstractEvalResult previousResult;
		if (element instanceof AbstractFormulaElement) {
			final IEvalElement formula = ((AbstractFormulaElement)element).getFormula();
			result = values.get(formula);
			previousResult = previousValues == null ? null : previousValues.get(formula);
		} else {
			result = null;
			previousResult = null;
		}
		
		return new ElementStateTreeItem(element.toString(), element, result, previousResult);
	}
	
	public AbstractEvalResult getResult() {
		return this.result;
	}
	
	public AbstractEvalResult getPreviousResult() {
		return this.previousResult;
	}
}
