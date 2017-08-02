package de.prob2.ui.states;

import de.prob.animator.domainobjects.StateError;
import de.prob.animator.prologast.ASTCategory;
import de.prob.animator.prologast.ASTFormula;
import javafx.scene.control.TreeTableCell;

import java.util.Objects;

class NameCell extends TreeTableCell<StateItem<?>, StateItem<?>> {
	public static String getName(final StateItem<?> item) {
		Objects.requireNonNull(item);
		
		final Object contents = item.getContents();
		
		if (contents instanceof String) {
			return (String)contents;
		} else if (contents instanceof ASTCategory) {
			return ((ASTCategory)contents).getName();
		} else if (contents instanceof ASTFormula) {
			return ((ASTFormula)contents).getFormula().toString();
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
