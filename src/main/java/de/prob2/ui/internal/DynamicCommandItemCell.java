package de.prob2.ui.internal;

import java.util.Arrays;

import de.prob.animator.domainobjects.DynamicCommandItem;
import javafx.scene.control.ListCell;

public final class DynamicCommandItemCell extends ListCell<DynamicCommandItem> {

	public DynamicCommandItemCell() {
		super();
		getStyleClass().add("dynamic-command-cell");
	}

	@Override
	protected void updateItem(DynamicCommandItem item, boolean empty) {
		super.updateItem(item, empty);
		this.getStyleClass().removeAll(Arrays.asList("dynamiccommandenabled", "dynamiccommanddisabled"));
		if (item != null && !empty) {
			setText(item.getName());
			if (item.isAvailable()) {
				getStyleClass().add("dynamiccommandenabled");
			} else {
				getStyleClass().add("dynamiccommanddisabled");
			}
		}
	}
}