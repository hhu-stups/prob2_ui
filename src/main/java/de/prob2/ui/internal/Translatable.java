package de.prob2.ui.internal;

@FunctionalInterface
public interface Translatable {

	String getTranslationKey();

	default Object[] getTranslationArguments() {
		return new Object[0];
	}
}
