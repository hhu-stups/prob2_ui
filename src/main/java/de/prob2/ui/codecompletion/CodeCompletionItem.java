package de.prob2.ui.codecompletion;

import javafx.scene.Node;

public interface CodeCompletionItem {

	/**
	 * Original text that needs to be replaced.
	 */
	String getOriginalText();

	/**
	 * Proposed replacement text.
	 */
	String getReplacement();

	/**
	 * Optional JavaFX node that gets shown in the CodeCompletion ListView.
	 * If {@literal null} is returned the string representation is shown instead.
	 */
	default Node getListNode() {
		return null;
	}
}
