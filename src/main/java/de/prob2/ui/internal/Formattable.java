package de.prob2.ui.internal;

public interface Formattable {

	String getFormattingPattern();

	default Object[] getFormattingArguments() {
		return new Object[0];
	}
}
