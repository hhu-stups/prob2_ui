package de.prob2.ui.consoles.b.codecompletion;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import de.prob.animator.command.CompleteIdentifierCommand;
import de.prob.statespace.StateSpace;

public final class BCodeCompletion {

	private final StateSpace stateSpace;
	private final String text;
	private final List<BCCItem> suggestions = new ArrayList<>();

	public BCodeCompletion(StateSpace stateSpace, String text) {
		this.stateSpace = stateSpace;
		this.text = text;
	}

	private static boolean isIdentifierStart(char c) {
		return Character.isJavaIdentifierStart(c) && !Character.isIdentifierIgnorable(c);
	}

	private static boolean isIdentifierPart(char c) {
		return Character.isJavaIdentifierPart(c) && !Character.isIdentifierIgnorable(c);
	}

	private static boolean isIdentifierChar(String text, int index) {
		char c = text.charAt(index);

		if (index == 0) {
			return isIdentifierStart(c);
		} else {
			char p = text.charAt(index - 1);
			if (isIdentifierStart(p) || isIdentifierPart(p)) {
				return isIdentifierPart(c);
			} else {
				return isIdentifierStart(c);
			}
		}
	}

	private static String extractPrefix(String text) {
		if (text.isEmpty()) {
			return "";
		}

		int first = text.length();
		while (first > 0 && isIdentifierChar(text, first - 1)) {
			first--;
		}

		return text.substring(first);
	}

	public static Collection<? extends BCCItem> doCompletion(StateSpace stateSpace, String text) {
		BCodeCompletion cc = new BCodeCompletion(stateSpace, extractPrefix(text));
		cc.find();
		return cc.getSuggestions();
	}

	public void find() {
		if (this.stateSpace != null) {
			CompleteIdentifierCommand cmd = new CompleteIdentifierCommand(this.text);
			cmd.setIgnoreCase(true);
			cmd.setIncludeKeywords(true);
			this.stateSpace.execute(cmd);
			this.suggestions.addAll(
				cmd.getCompletions().stream()
					.map(item -> new BCCItem(this.text, item))
					.collect(Collectors.toList())
			);
		}
	}

	public List<BCCItem> getSuggestions() {
		return this.suggestions;
	}
}
