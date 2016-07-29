package de.prob2.ui.states;

import de.prob.model.representation.AbstractElement;

public class ElementClassStateTreeItem extends StateTreeItem<Class<? extends AbstractElement>> {
	private static String formatClassName(final Class<?> clazz) {
		String shortName = clazz.getSimpleName();
		if (shortName.endsWith("y")) {
			shortName = shortName.substring(0, shortName.length() - 1) + "ies";
		} else {
			shortName += "s";
		}
		return shortName;
	}
	
	public ElementClassStateTreeItem(final Class<? extends AbstractElement> clazz) {
		super(formatClassName(clazz), "", "", clazz);
	}
}
