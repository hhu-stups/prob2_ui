package de.prob2.ui.states;

import java.util.List;

import de.prob.animator.domainobjects.BVisual2Formula;
import de.prob.animator.domainobjects.BVisual2Value;
import de.prob.animator.domainobjects.ExpandedFormula;
import de.prob.statespace.State;

// This class needs to be public (even though it's only used inside this package) so that Bindings.select can access its getters.
public final class StateItem {
	public interface FormulaEvaluator {
		public abstract ExpandedFormula expand(final BVisual2Formula formula);
		public abstract BVisual2Value evaluate(final BVisual2Formula formula, final State state);
	}

	private final BVisual2Formula formula;
	private final State currentState;
	private final State previousState;
	private final StateItem.FormulaEvaluator evaluator;
	private ExpandedFormula structure;
	private BVisual2Value currentValue;
	private BVisual2Value previousValue;

	StateItem(final BVisual2Formula formula, final State currentState, final State previousState, final StateItem.FormulaEvaluator evaluator) {
		this.formula = formula;
		this.currentState = currentState;
		this.previousState = previousState;
		this.evaluator = evaluator;
		this.structure = null;
		this.currentValue = null;
		this.previousValue = null;
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

	private ExpandedFormula getStructure() {
		if (this.structure == null) {
			this.structure = this.evaluator.expand(this.getFormula());
		}
		return this.structure;
	}

	public String getLabel() {
		return this.getStructure().getLabel();
	}

	public String getDescription() {
		return this.getStructure().getDescription();
	}

	public BVisual2Value getCurrentValue() {
		if (this.currentValue == null) {
			this.currentValue = this.evaluator.evaluate(this.getFormula(), this.getCurrentState());
		}
		return this.currentValue;
	}

	public BVisual2Value getPreviousValue() {
		if (this.previousValue == null) {
			if (this.getPreviousState() == null) {
				// Previous state not available, use an inactive value as a placeholder.
				this.previousValue = BVisual2Value.Inactive.INSTANCE;
			} else {
				this.previousValue = this.evaluator.evaluate(this.getFormula(), this.getPreviousState());
			}
		}
		return this.previousValue;
	}

	public List<BVisual2Formula> getSubformulas() {
		return this.getStructure().getSubformulas();
	}
}
