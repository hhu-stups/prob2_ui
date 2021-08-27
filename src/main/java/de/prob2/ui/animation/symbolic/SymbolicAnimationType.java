package de.prob2.ui.animation.symbolic;

import de.prob2.ui.symbolic.SymbolicExecutionType;

public enum SymbolicAnimationType implements SymbolicExecutionType {
	SEQUENCE("Sequence"),
	FIND_VALID_STATE("Find valid state"),
	;
	
	private final String name;
	
	SymbolicAnimationType(final String name) {
		this.name = name;
	}
	
	@Override
	public String getName() {
		return this.name;
	}
}
