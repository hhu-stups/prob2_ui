package de.prob2.ui.states;

import java.util.Objects;

import de.prob.animator.domainobjects.StateError;
import de.prob.model.representation.AbstractElement;

import javafx.scene.control.TreeTableCell;

class NameCell extends TreeTableCell<Object, Object> {
	public static String getName(final Object item) {
		Objects.requireNonNull(item);
		
		if (item instanceof String) {
			return (String)item;
		} else if (item instanceof Class<?>) {
			String shortName = ((Class<?>)item).getSimpleName();
			if (shortName.endsWith("y")) {
				shortName = shortName.substring(0, shortName.length() - 1) + "ies";
			} else {
				shortName += "s";
			}
			return shortName;
		} else if (item instanceof AbstractElement) {
			return item.toString();
		} else if (item instanceof StateError) {
			return ((StateError)item).getEvent();
		} else {
			throw new IllegalArgumentException("Don't know how to get the name of a " + item.getClass() + " instance");
		}
	}
	
	@Override
	protected void updateItem(final Object item, final boolean empty) {
		super.updateItem(item, empty);
		
		this.setText(item == null || empty ? null : getName(item));
		this.setGraphic(null);
	}
}
