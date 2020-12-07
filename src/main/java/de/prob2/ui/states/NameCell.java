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
			// If a functor symbol (top-level operator) is available,
			// display it before the formula text.
			final String functorSymbol = item.getFunctorSymbol();
			if (functorSymbol != null) {
				String text = "[" + functorSymbol + "]";
				// Display only the functor symbol if the item is expanded,
				// because the other parts of the formula will be visible as its children.
				if (!item.isExpanded()) {
					text += " " + item.getLabel();
				}
				this.setText(text);
			} else {
				this.setText(item.getLabel());
			}
			
			if (item.getCurrentValue() instanceof BVisual2Value.Inactive) {
				this.getStyleClass().add("not-initialized");
			} else if (item.getCurrentValue() instanceof BVisual2Value.Error) {
				this.getStyleClass().add("error");
			}
		}
		this.setGraphic(null);
	}
}
