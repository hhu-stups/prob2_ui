package de.prob2.ui.consoles.b.codecompletion;

import de.prob2.ui.codecompletion.CodeCompletionItem;

public class BCCItem implements CodeCompletionItem {

	private final String originalText;
	private final String replacement;
	private final String type;

	public BCCItem(String originalText, String replacement, String type) {
		this.originalText = originalText;
		this.replacement = replacement;
		this.type = type;
	}

	@Override
	public String getOriginalText() {
		return this.originalText;
	}

	public String getType() {
		return this.type;
	}

	@Override
	public String getReplacement() {
		return this.replacement;
	}

	@Override
	public String toString() {
		return this.getReplacement(); // + " " + this.getType();
	}
}
