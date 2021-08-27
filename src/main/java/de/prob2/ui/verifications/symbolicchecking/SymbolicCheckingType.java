package de.prob2.ui.verifications.symbolicchecking;

import de.prob2.ui.symbolic.SymbolicExecutionType;

public enum SymbolicCheckingType implements SymbolicExecutionType {
	INVARIANT("Invariant"),
	DEADLOCK("Deadlock"),
	CHECK_REFINEMENT("Refinement Checking"),
	CHECK_STATIC_ASSERTIONS("Static Assertion Checking"),
	CHECK_DYNAMIC_ASSERTIONS("Dynamic Assertion Checking"),
	CHECK_WELL_DEFINEDNESS("Well-Definedness Checking"),
	FIND_REDUNDANT_INVARIANTS("Find redundant invariants"),
	SYMBOLIC_MODEL_CHECK("Symbolic model checking"),
	;
	
	private final String name;
	
	SymbolicCheckingType(final String name) {
		this.name = name;
	}
	
	@Override
	public String getName() {
		return this.name;
	}
}
