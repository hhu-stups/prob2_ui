package de.prob2.ui.consoles.b.codecompletion;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
		return c == '@' || c == '\\' || (Character.isJavaIdentifierStart(c) && !Character.isIdentifierIgnorable(c));
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
		return doCompletion(stateSpace, text, true);
	}

	public static Collection<? extends BCCItem> doCompletion(StateSpace stateSpace, String text, boolean includeKeywords) {
		BCodeCompletion cc = new BCodeCompletion(stateSpace, extractPrefix(text));
		cc.find(true, includeKeywords);
		return cc.getSuggestions();
	}

	private void find(boolean ignoreCase, boolean includeKeywords) {
		if (this.stateSpace != null) {
			CompleteIdentifierCommand cmd = new CompleteIdentifierCommand(this.text);
			cmd.setIgnoreCase(ignoreCase);
			cmd.setKeywords(includeKeywords ? CompleteIdentifierCommand.KeywordContext.ALL : CompleteIdentifierCommand.KeywordContext.EXPR);
			this.stateSpace.execute(cmd);
			this.suggestions.addAll(cmd.getCompletions().stream().map(item -> new BCCItem(this.text, item)).toList());

			if (this.text.startsWith("\\")) {
				CompleteIdentifierCommand cmd2 = new CompleteIdentifierCommand(this.text.substring(1));
				cmd2.setIgnoreCase(ignoreCase);
				cmd2.setKeywords(CompleteIdentifierCommand.KeywordContext.LATEX);
				this.stateSpace.execute(cmd2);
				// TODO: get unicode replacement from Prolog
				this.suggestions.addAll(cmd2.getCompletions().stream().map(item -> new BCCItem(this.text, "\\" + item)).toList());
			}
		}
	}

	public List<BCCItem> getSuggestions() {
		return this.suggestions;
	}
}
