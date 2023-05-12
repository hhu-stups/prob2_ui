package de.prob2.ui.consoles.b.codecompletion;

import java.util.ArrayList;
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
