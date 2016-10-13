package de.prob2.ui.states;

import java.util.Map;
import java.util.Objects;

import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob.animator.domainobjects.EnumerationWarning;
import de.prob.animator.domainobjects.EvalResult;
import de.prob.animator.domainobjects.EvaluationErrorResult;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.animator.domainobjects.IdentifierNotInitialised;
import de.prob.model.representation.AbstractElement;
import de.prob.model.representation.AbstractFormulaElement;

public class ElementStateTreeItem extends StateTreeItem<AbstractElement> {
	public ElementStateTreeItem(final String name, final String value, final String previousValue, final AbstractElement contents) {
		super(name, value, previousValue, contents);
	}
	
	private static String stringRep(final AbstractEvalResult res) {
		Objects.requireNonNull(res);
		if (res instanceof IdentifierNotInitialised) {
			return "(not initialized)";
		} else if (res instanceof EvalResult) {
			return ((EvalResult) res).getValue();
		} else if (res instanceof EvaluationErrorResult) {
			return ((EvaluationErrorResult) res).getResult();
		} else if (res instanceof EnumerationWarning) {
			return "?(∞)";
		} else {
			return res.getClass() + " toString: " + res;
		}
	}
	
	public static ElementStateTreeItem fromElementAndValues(
		final AbstractElement element,
		final Map<IEvalElement, AbstractEvalResult> values,
		final Map<IEvalElement, AbstractEvalResult> previousValues
	) {
		final String value;
		final String previousValue;
		if (element instanceof AbstractFormulaElement) {
			final IEvalElement formula = ((AbstractFormulaElement)element).getFormula();
			value = stringRep(values.get(formula));
			previousValue = previousValues == null ? "" : stringRep(previousValues.get(formula));
		} else {
			value = "";
			previousValue = "";
		}
		
		return new ElementStateTreeItem(element.toString(), value, previousValue, element);
	}
}
