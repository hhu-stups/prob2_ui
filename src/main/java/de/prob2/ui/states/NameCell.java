package de.prob2.ui.states;

import java.util.Objects;

import de.prob.animator.domainobjects.StateError;
import de.prob.model.representation.AbstractElement;
import javafx.scene.control.TreeTableCell;

class NameCell extends TreeTableCell<StateItem<?>, StateItem<?>> {
	public static String getName(final StateItem<?> item) {
		Objects.requireNonNull(item);
		
		final Object contents = item.getContents();
		
		if (contents instanceof String) {
			return (String)contents;
		} else if (contents instanceof Class<?>) {
			String shortName = ((Class<?>)contents).getSimpleName();
			if (shortName.endsWith("y")) {
				shortName = shortName.substring(0, shortName.length() - 1) + "ies";
			} else {
				shortName += "s";
			}
			return shortName;
		} else if (contents instanceof AbstractElement) {
			return contents.toString();
		} else if (contents instanceof StateError) {
			return ((StateError)contents).getEvent();
		} else {
			throw new IllegalArgumentException("Don't know how to get the name of a " + contents.getClass() + " instance");
		}
	}
	
	@Override
	protected void updateItem(final StateItem<?> item, final boolean empty) {
		super.updateItem(item, empty);
		
		this.getStyleClass().removeAll("errorresult");
		
		if (item == null || empty) {
			this.setText(null);
		} else {
			this.setText(getName(item));
			if (item.isErrored()) {
				this.getStyleClass().add("errorresult");
			}
		}
		this.setGraphic(null);
	}
}
