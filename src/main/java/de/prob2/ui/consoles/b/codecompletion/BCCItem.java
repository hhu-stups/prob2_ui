package de.prob2.ui.consoles.b.codecompletion;

import de.prob2.ui.codecompletion.CodeCompletionItem;

public class BCCItem implements CodeCompletionItem {

	private final String originalText;
	private final String replacement;

	public BCCItem(String originalText, String replacement) {
		this.originalText = originalText;
		this.replacement = replacement;
	}

	@Override
	public String getOriginalText() {
		return this.originalText;
	}

	@Override
	public String getReplacement() {
		return this.replacement;
	}

	@Override
	public String toString() {
		return this.getReplacement();
	}
}
