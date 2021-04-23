package de.prob2.ui.animation.symbolic.testcasegeneration;

public enum TestCaseGenerationType {
	// Note: When adding/changing/removing enum constants here,
	// make sure to also adjust TestCaseGenerationItem,
	// especially the internal class TestCaseGenerationItem.AdditionalInformation and its subclasses.
	MCDC("MCDC Testing"),
	COVERED_OPERATIONS("Covered Operations Testing")
	;
	
	private final String name;
	
	TestCaseGenerationType(final String name) {
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
