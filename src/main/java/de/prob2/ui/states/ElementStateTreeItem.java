package de.prob2.ui.states;

import de.prob.model.representation.AbstractElement;
import de.prob.model.representation.AbstractFormulaElement;
import de.prob.statespace.Trace;

public class ElementStateTreeItem extends StateTreeItem<AbstractElement> {
	public ElementStateTreeItem(final Trace trace, final AbstractElement element) {
		super(element.toString(), "", "", element);
		this.update(trace);
	}
	
	@Override
	public void update(final Trace trace) {
		this.value.set(
			this.getContents() instanceof AbstractFormulaElement
			? StatesView.stringRep(trace.getCurrentState().eval(((AbstractFormulaElement)this.getContents()).getFormula()))
			: ""
		);
		this.previousValue.set(
			this.getContents() instanceof AbstractFormulaElement && trace.canGoBack()
			? StatesView.stringRep(trace.getPreviousState().eval(((AbstractFormulaElement)this.getContents()).getFormula()))
			: ""
		);
	}
}
