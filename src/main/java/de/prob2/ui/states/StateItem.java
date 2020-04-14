package de.prob2.ui.states;

import de.prob.animator.domainobjects.BVisual2Formula;
import de.prob.animator.domainobjects.BVisual2Value;
import de.prob.animator.domainobjects.ExpandedFormula;
import de.prob.statespace.State;

// This class needs to be public (even though it's only used inside this package) so that Bindings.select can access its getters.
public final class StateItem {
	private final BVisual2Formula formula;
	private final State currentState;
	private final State previousState;
	private ExpandedFormula current;
	private ExpandedFormula previous;

	StateItem(final BVisual2Formula formula, final State currentState, final State previousState) {
		this.formula = formula;
		this.currentState = currentState;
		this.previousState = previousState;
		this.current = null;
		this.previous = null;
	}

	public BVisual2Formula getFormula() {
		return this.formula;
	}

	public State getCurrentState() {
		return this.currentState;
	}

	public State getPreviousState() {
		return this.previousState;
	}

	private void evaluate() {
		this.current = this.getFormula().expandNonrecursive(this.getCurrentState());
		if (this.getPreviousState() == null) {
			// Previous state not available, use a placeholder formula with an inactive value.
			this.previous = ExpandedFormula.withoutChildren(this.getCurrent().getFormula(), this.getCurrent().getLabel(), BVisual2Value.Inactive.INSTANCE);
		} else {
			this.previous = this.getFormula().expandNonrecursive(this.getPreviousState());
		}
	}

	public ExpandedFormula getCurrent() {
		if (this.current == null) {
			this.evaluate();
		}
		return this.current;
	}

	public ExpandedFormula getPrevious() {
		if (this.previous == null) {
			this.evaluate();
		}
		return this.previous;
	}
}
