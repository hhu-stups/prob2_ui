package de.prob2.ui.simulation.configuration;

import com.fasterxml.jackson.annotation.JsonValue;

public enum TransitionSelection {

	/**
	 * Always choose first matching transition.
	 */
	FIRST("first"),
	/**
	 * Select one transition uniformly from all matching transitions.
	 */
	UNIFORM("uniform");

	private final String name;

	TransitionSelection(String name) {
		this.name = name;
	}

	@JsonValue
	public String getName() {
		return this.name;
	}

	public static TransitionSelection fromName(String name) {
		return switch (name) {
			case "first" -> FIRST;
			case "uniform" -> UNIFORM;
			default -> throw new IllegalArgumentException("Unknown ProbabilisticVariables value: " + name);
		};
	}
}
