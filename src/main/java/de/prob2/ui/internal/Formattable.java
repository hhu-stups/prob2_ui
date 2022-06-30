package de.prob2.ui.internal;

@FunctionalInterface
public interface Formattable {

	String getFormattingPattern();

	default Object[] getFormattingArguments() {
		return new Object[0];
	}
}
