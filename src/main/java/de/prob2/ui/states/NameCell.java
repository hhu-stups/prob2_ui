package de.prob2.ui.states;

import de.prob.animator.domainobjects.BVisual2Value;

import javafx.scene.control.TreeTableCell;

class NameCell extends TreeTableCell<StateItem, StateItem> {
	@Override
	protected void updateItem(final StateItem item, final boolean empty) {
		super.updateItem(item, empty);
		
		this.getStyleClass().removeAll("not-initialized", "error");
		
		if (item == null || empty) {
			this.setText(null);
		} else {
			this.setText(item.getLabel());
			if (item.getCurrentValue() instanceof BVisual2Value.Inactive) {
				this.getStyleClass().add("not-initialized");
			} else if (item.getCurrentValue() instanceof BVisual2Value.Error) {
				this.getStyleClass().add("error");
			}
		}
		this.setGraphic(null);
	}
}
