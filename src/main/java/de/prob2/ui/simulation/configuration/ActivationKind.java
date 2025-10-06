package de.prob2.ui.simulation.configuration;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ActivationKind {

	SINGLE("single"),
	SINGLE_MIN("single:min"),
	SINGLE_MAX("single:max"),
	MULTI("multi");

	private final String name;

	ActivationKind(String name) {
		this.name = name;
	}

	@JsonValue
	public String getName() {
		return this.name;
	}

	public static ActivationKind fromName(String name) {
		return switch (name) {
			case "single" -> SINGLE;
			case "single:min" -> SINGLE_MIN;
			case "single:max" -> SINGLE_MAX;
			case "multi" -> MULTI;
			default -> throw new IllegalArgumentException("Unknown activation kind: " + name);
		};
	}
}
