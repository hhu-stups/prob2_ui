package de.prob2.ui.states;

import java.util.Objects;

import de.prob.animator.prologast.ASTCategory;
import de.prob.animator.prologast.ASTFormula;
import de.prob.animator.prologast.PrologASTNode;

import javafx.scene.control.TreeTableCell;

class NameCell extends TreeTableCell<StateItem<?>, StateItem<?>> {
	public static String getName(final StateItem<?> item) {
		Objects.requireNonNull(item);
		
		final PrologASTNode contents = item.getContents();
		
		if (contents instanceof ASTCategory) {
			return ((ASTCategory)contents).getName();
		} else if (contents instanceof ASTFormula) {
			return ((ASTFormula)contents).getFormula().toString();
		} else {
			throw new IllegalArgumentException("Don't know how to get the name of a " + contents.getClass() + " instance");
		}
	}
	
	@Override
	protected void updateItem(final StateItem<?> item, final boolean empty) {
		super.updateItem(item, empty);
		
		this.getStyleClass().removeAll("error");
		
		if (item == null || empty) {
			this.setText(null);
		} else {
			this.setText(getName(item));
			if (item.isErrored()) {
				this.getStyleClass().add("error");
			}
		}
		this.setGraphic(null);
	}
}
