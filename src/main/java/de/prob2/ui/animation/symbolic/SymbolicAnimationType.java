package de.prob2.ui.animation.symbolic;

import de.prob2.ui.symbolic.SymbolicExecutionType;

public enum SymbolicAnimationType implements SymbolicExecutionType {

	SEQUENCE("animation.type.sequence"),
	FIND_VALID_STATE("animation.type.findValidState"),
	;

	private final String translationKey;

	SymbolicAnimationType(final String translationKey) {
		this.translationKey = translationKey;
	}

	@Override
	public String getTranslationKey() {
		return this.translationKey;
	}
}
