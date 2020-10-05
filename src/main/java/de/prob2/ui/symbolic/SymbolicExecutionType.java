package de.prob2.ui.symbolic;

public enum SymbolicExecutionType {
	
	SEQUENCE("Sequence"),
	INVARIANT("Invariant"),
	DEADLOCK("Deadlock"),
	FIND_VALID_STATE("Find valid state"),
	FIND_REDUNDANT_INVARIANTS("Find redundant invariants"),
	CHECK_REFINEMENT("Refinement Checking"),
	CHECK_STATIC_ASSERTIONS("Static Assertion Checking"),
	CHECK_DYNAMIC_ASSERTIONS("Dynamic Assertion Checking"),
	CHECK_WELL_DEFINEDNESS("Well-Definedness Checking"),
	SYMBOLIC_MODEL_CHECK("Symbolic model checking"),
	;
	
	private final String name;
	
	SymbolicExecutionType(final String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	
	@Override
	public String toString() {
		return name;
	}
}
