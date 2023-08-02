package de.prob2.ui.simulation.choice;

import de.prob2.ui.internal.I18n;

public enum SimulationCheckingType {
	ALL_INVARIANTS("simulation.type.property.invariants"),
	PREDICATE_INVARIANT("simulation.type.property.invariant"),
	PREDICATE_FINAL("simulation.type.property.endPredicate"),
	PREDICATE_EVENTUALLY("simulation.type.property.eventually"),
	TIMING("simulation.type.property.timing"),
	AVERAGE("simulation.type.property.average"),
	SUM("simulation.type.property.sum");

	private final String key;

	SimulationCheckingType(String key) {
		this.key = key;
	}

	public String getKey() {
		return key;
	}

	public String getName(I18n i18n) {
		return i18n.translate(key);
	}
}
