package de.prob2.ui.internal;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.fxmisc.richtext.GenericStyledArea;

public class CachedEditorState {

	private Path selectedMachine;
	private final Map<Path, TextAreaState> textAreaStates = new HashMap<>();

	public Path getSelectedMachine() {
		return selectedMachine;
	}

	public TextAreaState getTextAreaState(Path machine) {
		return textAreaStates.get(machine);
	}

	public void setSelectedMachine(Path selectedMachine) {
		this.selectedMachine = selectedMachine;
	}

	public void setTextAreaState(Path machine, GenericStyledArea<?, ?, ?> textArea) {
		setTextAreaState(machine, TextAreaState.from(textArea));
	}

	public void setTextAreaState(Path machine, TextAreaState textAreaState) {
		textAreaStates.put(machine, textAreaState);
	}

	public void setCaretPosition(Path machine, int caretPosition) {
		TextAreaState textAreaState = textAreaStates.get(machine);
		if (textAreaState == null) {
			textAreaState = new TextAreaState(caretPosition, 0, 0);
		} else {
			textAreaState = textAreaState.withCaretPosition(caretPosition);
		}

		textAreaStates.put(machine, textAreaState);
	}

	public void setScrollXPosition(Path machine, double scrollXPosition) {
		TextAreaState textAreaState = textAreaStates.get(machine);
		if (textAreaState == null) {
			textAreaState = new TextAreaState(0, scrollXPosition, 0);
		} else {
			textAreaState = textAreaState.withScrollXPosition(scrollXPosition);
		}

		textAreaStates.put(machine, textAreaState);
	}

	public void setScrollYPosition(Path machine, double scrollYPosition) {
		TextAreaState textAreaState = textAreaStates.get(machine);
		if (textAreaState == null) {
			textAreaState = new TextAreaState(0, 0, scrollYPosition);
		} else {
			textAreaState = textAreaState.withScrollYPosition(scrollYPosition);
		}

		textAreaStates.put(machine, textAreaState);
	}
}
