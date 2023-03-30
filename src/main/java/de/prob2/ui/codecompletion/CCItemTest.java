package de.prob2.ui.codecompletion;

public class CCItemTest implements CodeCompletionItem {

	private final String text;

	public CCItemTest(String text) {
		this.text = text;
	}

	public String getText() {
		return text;
	}

	@Override
	public String toString() {
		return getText();
	}
}
