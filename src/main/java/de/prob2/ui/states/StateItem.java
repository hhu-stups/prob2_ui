package de.prob2.ui.states;

import de.prob.animator.domainobjects.ExpandedFormula;

// This class needs to be public (even though it's only used inside this package) so that Bindings.select can access its getters.
public final class StateItem {
	private final ExpandedFormula current;
	private final ExpandedFormula previous;
	
	StateItem(final ExpandedFormula current, final ExpandedFormula previous) {
		this.current = current;
		this.previous = previous;
	}
	
	public ExpandedFormula getCurrent() {
		return this.current;
	}
	
	public ExpandedFormula getPrevious() {
		return this.previous;
	}
}
