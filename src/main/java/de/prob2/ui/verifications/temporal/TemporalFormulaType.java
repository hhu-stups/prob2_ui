package de.prob2.ui.verifications.temporal;

import de.prob2.ui.internal.Translatable;

public enum TemporalFormulaType implements Translatable {

	LTL("verifications.temporal.type"),
	CTL("verifications.temporal.type"),
	;

	private final String translationKey;

	TemporalFormulaType(final String translationKey) {
		this.translationKey = translationKey;
	}

	@Override
	public String getTranslationKey() {
		return this.translationKey;
	}
}
