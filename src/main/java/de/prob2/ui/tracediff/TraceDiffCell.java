package de.prob2.ui.tracediff;

import javafx.scene.control.ListCell;

class TraceDiffCell extends ListCell<TraceDiffItem> {
	TraceDiffCell() {
		getStyleClass().add("trace-diff-cell");
	}

	@Override
	protected void updateItem(TraceDiffItem item, boolean empty) {
		super.updateItem(item, empty);
		getStyleClass().removeAll("faulty", "following");
		if (item != null){
			setText(item.getString());
			if (item.getId() == TraceDiff.getMinSize()) {
				getStyleClass().add("faulty");
			} else if (item.getId() > TraceDiff.getMinSize()) {
				getStyleClass().add("following");
			}
			this.setHeight(item.getString().split("\n").length * 15);
		} else {
			setText(null);
		}
	}
}
