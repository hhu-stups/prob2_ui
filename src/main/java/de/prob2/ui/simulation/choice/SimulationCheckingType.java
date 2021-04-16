package de.prob2.ui.simulation.choice;

public enum SimulationCheckingType {
	ALL_INVARIANTS("All Invariants"),
	PREDICATE_INVARIANT("Predicate as Invariant"),
	PREDICATE_FINAL("Final Predicate"),
	PREDICATE_EVENTUALLY("Predicate Eventually"),
	TIMING("Timing");

	private final String name;

	SimulationCheckingType(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
}
