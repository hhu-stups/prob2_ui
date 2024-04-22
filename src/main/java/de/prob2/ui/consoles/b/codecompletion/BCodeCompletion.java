package de.prob2.ui.consoles.b.codecompletion;

import java.util.ArrayList;
import java.util.List;

import de.prob.animator.command.CompleteIdentifierCommand;
import de.prob.model.brules.RulesModel;
import de.prob.model.classicalb.ClassicalBModel;
import de.prob.model.eventb.EventBModel;
import de.prob.model.representation.AbstractModel;
import de.prob.model.representation.AlloyModel;
import de.prob.model.representation.XTLModel;
import de.prob.model.representation.ZModel;
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

	public static List<? extends BCCItem> doCompletion(StateSpace stateSpace, String text) {
		return doCompletion(stateSpace, text, true);
	}

	public static List<? extends BCCItem> doCompletion(StateSpace stateSpace, String text, boolean inEditor) {
		BCodeCompletion cc = new BCodeCompletion(stateSpace, extractPrefix(text));
		cc.find(true, inEditor);
		return cc.getSuggestions();
	}

	private void find(boolean ignoreCase, boolean inEditor) {
		if (this.stateSpace != null) {
			boolean allowUnicodeConversions = false;
			boolean allowLatex = false;
			AbstractModel m = this.stateSpace.getModel();
			if (m instanceof ClassicalBModel || m instanceof RulesModel || m instanceof EventBModel) {
				allowUnicodeConversions = true;
				allowLatex = true;
			} else if (!inEditor && (m instanceof AlloyModel || m instanceof XTLModel || m instanceof ZModel)) {
				// the console is in Classical B mode
				allowUnicodeConversions = true;
				allowLatex = true;
			}

			CompleteIdentifierCommand cmd = new CompleteIdentifierCommand(this.text);
			cmd.setIgnoreCase(ignoreCase);
			cmd.setLatexToUnicode(allowUnicodeConversions);
			cmd.setAsciiToUnicode(allowUnicodeConversions); // TODO: include special chars like !, # or 1 for ascii to unicode conversion
			cmd.addKeywordContext(inEditor ? CompleteIdentifierCommand.KeywordContext.ALL : CompleteIdentifierCommand.KeywordContext.EXPR);
			if (allowLatex) {
				cmd.addKeywordContext(CompleteIdentifierCommand.KeywordContext.LATEX);
			}
			this.stateSpace.execute(cmd);
			// TODO: update this once kernel update releases
			this.suggestions.addAll(cmd.getCompletions().stream().map(item -> new BCCItem(this.text, item /*item.getCompletion(), item.getType()*/)).toList());
			// TODO: convert latex commands into unicode directly
		}
	}

	public List<BCCItem> getSuggestions() {
		return this.suggestions;
	}
}
