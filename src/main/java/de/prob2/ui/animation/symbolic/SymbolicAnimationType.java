package de.prob2.ui.animation.symbolic;


public enum SymbolicAnimationType {
	
	SEQUENCE("Sequence"),
	DEADLOCK("Deadlock"),
	FIND_DEADLOCK("Find deadlock"),
	FIND_VALID_STATE("Find valid state"), 
	FIND_REDUNDANT_INVARIANTS("Find redundant invariants"), 
	;
	
	private final String name;
	
	SymbolicAnimationType(final String name) {
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
