package de.prob2.ui.animation.symbolic.testcasegeneration;

import de.prob2.ui.internal.Translatable;

public enum TestCaseGenerationType implements Translatable {

	MCDC("animation.testcase.type.mcdc"),
	COVERED_OPERATIONS("animation.testcase.type.coveredOperations");

	private final String translationKey;

	TestCaseGenerationType(final String translationKey) {
		this.translationKey = translationKey;
	}

	@Override
	public String getTranslationKey() {
		return translationKey;
	}
}
