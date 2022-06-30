package de.prob2.ui.internal;

public interface Translatable {

	String getTranslationKey();

	default Object[] getFormattingArguments() {
		return new Object[0];
	}
}
