package de.prob2.ui.verifications.symbolicchecking;

import de.prob.animator.command.SymbolicModelcheckCommand;

public enum SymbolicCheckingType {
	
	SEQUENCE("", null),
	INVARIANT("", null),
	DEADLOCK("", null),
	FIND_DEADLOCK("FIND DEADLOCK", null),
	FIND_VALID_STATE("", null), 
	CHECK_ALL_OPERATIONS("", null), 
	FIND_REDUNDANT_INVARIANTS("FIND REDUNDANT INVARIANTS", null), 
	CHECK_REFINEMENT("Refinement Checking", null), 
	CHECK_ASSERTIONS("Assertion Checking", null), 
	IC3("IC3", SymbolicModelcheckCommand.Algorithm.IC3), 
	TINDUCTION("TINDUCTION", SymbolicModelcheckCommand.Algorithm.TINDUCTION), 
	KINDUCTION("KINDUCTION", SymbolicModelcheckCommand.Algorithm.KINDUCTION), 
	BMC("BMC", SymbolicModelcheckCommand.Algorithm.BMC)
	;
	
	private final String name;
	
	private final SymbolicModelcheckCommand.Algorithm algorithm;
	
	SymbolicCheckingType(final String name, final SymbolicModelcheckCommand.Algorithm algorithm) {
		this.name = name;
		this.algorithm = algorithm;
	}
	
	public String getName() {
		return this.name;
	}
	
	public SymbolicModelcheckCommand.Algorithm getAlgorithm() {
		return algorithm;
	}
}
