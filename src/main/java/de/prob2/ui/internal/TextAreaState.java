package de.prob2.ui.internal;

import org.fxmisc.richtext.GenericStyledArea;

public class TextAreaState {

	public final int caretPosition;
	public final double scrollXPosition;
	public final double scrollYPosition;

	public TextAreaState(int caretPosition, double scrollXPosition, double scrollYPosition) {
		this.caretPosition = caretPosition;
		this.scrollXPosition = scrollXPosition;
		this.scrollYPosition = scrollYPosition;
	}

	public static TextAreaState from(GenericStyledArea<?, ?, ?> textArea) {
		return new TextAreaState(textArea.getCaretPosition(), textArea.getEstimatedScrollX(), textArea.getEstimatedScrollY());
	}

	public TextAreaState withCaretPosition(int caretPosition) {
		return new TextAreaState(caretPosition, scrollXPosition, scrollYPosition);
	}

	public TextAreaState withScrollXPosition(double scrollXPosition) {
		return new TextAreaState(caretPosition, scrollXPosition, scrollYPosition);
	}

	public TextAreaState withScrollYPosition(double scrollYPosition) {
		return new TextAreaState(caretPosition, scrollXPosition, scrollYPosition);
	}
}
