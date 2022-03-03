package de.prob2.ui.vomanager;

public enum RequirementType {
	FUNCTIONAL("Functional Requirement"),
	NON_FUNCTIONAL("Non-Functional Requirement");

	private final String name;

	RequirementType(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return getName();
	}
}
