package de.prob2.ui.verifications.temporal;

import de.prob2.ui.internal.Translatable;

public enum TemporalFormulaType implements Translatable {
	LTL("verifications.temporal.type.ltl"),
	CTL("verifications.temporal.type.ctl"),
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
