package de.prob2.ui.internal;

import java.util.Arrays;

import de.prob.animator.domainobjects.DynamicCommandItem;
import javafx.scene.control.ListCell;

public final class DynamicCommandItemCell extends ListCell<DynamicCommandItem> {
	
	private final String styleClassEnabled;
	
	private final String styleClassDisabled;

	public DynamicCommandItemCell(String styleClass, String styleClassEnabled, String styleClassDisabled) {
		super();
		this.styleClassEnabled = styleClassEnabled;
		this.styleClassDisabled = styleClassDisabled;
		getStyleClass().add(styleClass);
	}

	@Override
	protected void updateItem(DynamicCommandItem item, boolean empty) {
		super.updateItem(item, empty);
		this.getStyleClass().removeAll(Arrays.asList(styleClassEnabled, styleClassDisabled));
		if (item != null && !empty) {
			setText(item.getName());
			if (item.isAvailable()) {
				getStyleClass().add(styleClassEnabled);
			} else {
				getStyleClass().add(styleClassDisabled);
			}
		}
	}
}