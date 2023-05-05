package de.prob2.ui.consoles.b.codecompletion;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import de.prob.model.representation.AbstractModel;
import de.prob.model.representation.Machine;
import de.prob.model.representation.ModelElementList;
import de.prob.model.representation.Variable;

public final class BCodeCompletion {

	private final AbstractModel model;
	private final String text;
	private final List<BCCItem> suggestions = new ArrayList<>();

	public BCodeCompletion(AbstractModel model, String text) {
		this.model = model;
		this.text = text;
	}

	public void find() {
		ModelElementList<Machine> machines = this.model.getChildrenOfType(Machine.class);
		List<? extends Variable> variables = machines.stream().flatMap(m -> m.getVariables().stream()).collect(Collectors.toList());
		for (Variable variable : variables) {
			this.addSuggestion(variable.getName());
		}
	}

	private void addSuggestion(String item) {
		if (item.toLowerCase(Locale.ROOT).startsWith(this.text.toLowerCase(Locale.ROOT))) {
			suggestions.add(new BCCItem(this.text, item));
		}
	}

	public List<BCCItem> getSuggestions() {
		return this.suggestions;
	}
}
