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
	
	public ElementStateTreeItem(
		final AbstractElement element,
		final Map<IEvalElement, AbstractEvalResult> values,
		final Map<IEvalElement, AbstractEvalResult> previousValues
	) {
		super(element.toString(), "", "", element);
		this.update(values, previousValues);
	}
	
	@Override
	public void update(
		final Map<IEvalElement, AbstractEvalResult> values,
		final Map<IEvalElement, AbstractEvalResult> previousValues
	) {
		if (this.getContents() instanceof AbstractFormulaElement) {
			IEvalElement formula = ((AbstractFormulaElement)this.getContents()).getFormula();
			this.value.set(stringRep(values.get(formula)));
			this.previousValue.set(previousValues == null ? "" : stringRep(previousValues.get(formula)));
		}
	}
}
