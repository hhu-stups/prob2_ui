package de.prob2.ui.states;

import de.prob.animator.domainobjects.StateError;

public class ErrorStateTreeItem extends StateTreeItem<StateError> {
	public ErrorStateTreeItem(final StateError error) {
		super(error.getEvent(), error.getShortDescription(), "", error);
	}
}
