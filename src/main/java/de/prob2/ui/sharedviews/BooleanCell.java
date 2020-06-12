package de.prob2.ui.sharedviews;

import javafx.scene.control.CheckBox;
import javafx.scene.control.TableCell;

public final class BooleanCell<T> extends TableCell<T, Boolean> {
	private final CheckBox checkBox;
	
	public BooleanCell() {
		super();
		
		this.setText(null);
		this.checkBox = new CheckBox();
		this.checkBox.setDisable(true);
		this.setGraphic(this.checkBox);
	}
	
	@Override
	protected void updateItem(final Boolean item, final boolean empty) {
		super.updateItem(item, empty);
		
		if (empty || item == null) {
			this.checkBox.setVisible(false);
		} else {
			this.checkBox.setVisible(true);
			this.checkBox.setSelected(item);
		}
	}
}
