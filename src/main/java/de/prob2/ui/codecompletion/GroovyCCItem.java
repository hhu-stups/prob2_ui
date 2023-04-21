package de.prob2.ui.codecompletion;

public class GroovyCCItem implements CodeCompletionItem {

	private final String originalText;
	private final String replacement;

	public GroovyCCItem(String originalText, String replacement) {
		this.originalText = originalText;
		this.replacement = replacement;
	}

	public String getOriginalText() {
		return originalText;
	}

	public String getReplacement() {
		return replacement;
	}

	@Override
	public String toString() {
		return getReplacement();
	}
}
