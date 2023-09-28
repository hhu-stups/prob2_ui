package de.prob2.ui.vomanager;

public enum VOManagerSetting {
	MACHINE("Machine"),
	REQUIREMENT("Requirement");

	private final String name;

	VOManagerSetting(String name) {
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
