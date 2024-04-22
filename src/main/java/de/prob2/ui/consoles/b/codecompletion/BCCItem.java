package de.prob2.ui.consoles.b.codecompletion;

import java.util.Objects;

import de.prob2.ui.codecompletion.CodeCompletionItem;

public class BCCItem implements CodeCompletionItem {

	private final String originalText;
	private final String replacement;
	private final String type;

	public BCCItem(String originalText, String replacement) {
		this(originalText, replacement, null);
	}

	public BCCItem(String originalText, String replacement, String type) {
		this.originalText = Objects.requireNonNull(originalText, "originalText");
		this.replacement = Objects.requireNonNull(originalText, "replacement");
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
