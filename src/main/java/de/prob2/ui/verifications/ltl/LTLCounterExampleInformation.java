package de.prob2.ui.verifications.ltl;

import de.prob.check.LTLCounterExample;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;

public class LTLCounterExampleInformation {
	
	private final LTLCounterExample example;
	private final StateSpace stateSpace;

	public LTLCounterExampleInformation(LTLCounterExample example, StateSpace stateSpace) {
		this.example = example;
		this.stateSpace = stateSpace;
	}
	
	public Trace getTrace() {
		return example.getTrace(stateSpace);
	}
	
}
