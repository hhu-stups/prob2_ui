package de.prob2.ui.consoles.groovy.codecompletion;

import de.prob2.ui.codecompletion.CodeCompletionItem;

public final class GroovyCCItem implements CodeCompletionItem {

	private final String originalText;
	private final String replacement;

	public GroovyCCItem(String originalText, String replacement) {
		this.originalText = originalText;
		this.replacement = replacement;
	}

	@Override
	public String getOriginalText() {
		return originalText;
	}

	@Override
	public String getReplacement() {
		return replacement;
	}

	@Override
	public String toString() {
		return this.getReplacement();
	}
}
