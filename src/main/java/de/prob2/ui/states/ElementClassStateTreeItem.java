package de.prob2.ui.states;

import de.prob.model.representation.AbstractElement;

public class ElementClassStateTreeItem extends StateTreeItem<Class<? extends AbstractElement>> {
	public ElementClassStateTreeItem(final Class<? extends AbstractElement> clazz) {
		super(StatesView.formatClassName(clazz, true), "", "", clazz);
	}
}
