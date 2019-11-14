package de.prob2.ui.states;

import de.prob.animator.domainobjects.BVisual2Value;
import de.prob.animator.domainobjects.ExpandedFormula;

import javafx.scene.control.TreeTableCell;

class NameCell extends TreeTableCell<StateItem, ExpandedFormula> {
	@Override
	protected void updateItem(final ExpandedFormula item, final boolean empty) {
		super.updateItem(item, empty);
		
		this.getStyleClass().removeAll("not-initialized", "error");
		
		if (item == null || empty) {
			this.setText(null);
		} else {
			this.setText(item.getLabel());
			if (item.getValue() instanceof BVisual2Value.Inactive) {
				this.getStyleClass().add("not-initialized");
			} else if (item.getValue() instanceof BVisual2Value.Error) {
				this.getStyleClass().add("error");
			}
		}
		this.setGraphic(null);
	}
}
