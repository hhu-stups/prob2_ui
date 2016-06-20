package de.prob2.ui.states;

import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.model.representation.AbstractElement;
import de.prob.model.representation.AbstractFormulaElement;

import java.util.Map;

public class ElementStateTreeItem extends StateTreeItem<AbstractElement> {
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
			this.value.set(StatesView.stringRep(values.get(formula)));
			this.previousValue.set(previousValues == null ? "" : StatesView.stringRep(previousValues.get(formula)));
		}
	}
}
