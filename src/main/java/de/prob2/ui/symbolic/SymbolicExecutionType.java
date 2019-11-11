package de.prob2.ui.symbolic;

import de.prob.animator.command.SymbolicModelcheckCommand;

public enum SymbolicExecutionType {
	
	SEQUENCE("Sequence", null),
	INVARIANT("Invariant", null),
	DEADLOCK("Deadlock", null),
	FIND_VALID_STATE("Find valid state", null), 
	CHECK_ALL_OPERATIONS("Check all operations", null), 
	FIND_REDUNDANT_INVARIANTS("Find redundant invariants", null), 
	CHECK_REFINEMENT("Refinement Checking", null), 
	CHECK_ASSERTIONS("Assertion Checking", null), 
	IC3("IC3", SymbolicModelcheckCommand.Algorithm.IC3), 
	TINDUCTION("TINDUCTION", SymbolicModelcheckCommand.Algorithm.TINDUCTION), 
	KINDUCTION("KINDUCTION", SymbolicModelcheckCommand.Algorithm.KINDUCTION), 
	BMC("BMC", SymbolicModelcheckCommand.Algorithm.BMC),
	;
	
	private final String name;
	
	private final SymbolicModelcheckCommand.Algorithm algorithm;
	
	SymbolicExecutionType(final String name, final SymbolicModelcheckCommand.Algorithm algorithm) {
		this.name = name;
		this.algorithm = algorithm;
	}
	
	public String getName() {
		return name;
	}
	
	public SymbolicModelcheckCommand.Algorithm getAlgorithm() {
		return algorithm;
	}
	
	@Override
	public String toString() {
		return name;
	}
}