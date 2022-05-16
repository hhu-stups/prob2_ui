package de.prob2.ui.animation.symbolic.testcasegeneration;

public enum TestCaseGenerationType {

	MCDC("animation.testcase.type.mcdc"),
	COVERED_OPERATIONS("animation.testcase.type.coveredOperations");

	private final String translationKey;

	TestCaseGenerationType(final String translationKey) {
		this.translationKey = translationKey;
	}

	public String getTranslationKey() {
		return translationKey;
	}
}
