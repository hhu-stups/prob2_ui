package de.prob2.ui.states;

import java.util.List;

import de.prob.animator.domainobjects.BVisual2Formula;
import de.prob.animator.domainobjects.BVisual2Value;
import de.prob.animator.domainobjects.ExpandedFormula;
import de.prob.statespace.State;

// This class needs to be public (even though it's only used inside this package) so that Bindings.select can access its getters.
public final class StateItem {
	@FunctionalInterface
	public interface FormulaEvaluator {
		public abstract ExpandedFormula evaluate(final BVisual2Formula formula, final State state);
	}

	private final BVisual2Formula formula;
	private final State currentState;
	private final State previousState;
	private final StateItem.FormulaEvaluator evaluator;
	private ExpandedFormula current;
	private ExpandedFormula previous;

	StateItem(final BVisual2Formula formula, final State currentState, final State previousState, final StateItem.FormulaEvaluator evaluator) {
		this.formula = formula;
		this.currentState = currentState;
		this.previousState = previousState;
		this.evaluator = evaluator;
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

	private ExpandedFormula getCurrent() {
		if (this.current == null) {
			this.current = this.evaluator.evaluate(this.getFormula(), this.getCurrentState());
		}
		return this.current;
	}

	private ExpandedFormula getPrevious() {
		if (this.previous == null) {
			if (this.getPreviousState() == null) {
				// Previous state not available, use a placeholder formula with an inactive value.
				this.previous = ExpandedFormula.withoutChildren(this.getCurrent().getFormula(), this.getCurrent().getLabel(), BVisual2Value.Inactive.INSTANCE);
			} else {
				this.previous = this.evaluator.evaluate(this.getFormula(), this.getPreviousState());
			}
		}
		return this.previous;
	}

	public String getLabel() {
		return this.getCurrent().getLabel();
	}

	public BVisual2Value getCurrentValue() {
		return this.getCurrent().getValue();
	}

	public BVisual2Value getPreviousValue() {
		return this.getPrevious().getValue();
	}

	public List<BVisual2Formula> getSubformulas() {
		return this.getCurrent().getSubformulas();
	}
}
