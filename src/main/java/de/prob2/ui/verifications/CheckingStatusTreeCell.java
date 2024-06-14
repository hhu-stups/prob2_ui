package de.prob2.ui.verifications;

import javafx.scene.control.TreeTableCell;

public final class CheckingStatusTreeCell<T> extends TreeTableCell<T, CheckingStatus> {
	public CheckingStatusTreeCell() {
		super();
		
		this.setText(null);
		CheckingStatusIcon iconView = new CheckingStatusIcon();
		iconView.setVisible(false);
		this.setGraphic(iconView);
	}
	
	@Override
	protected void updateItem(CheckingStatus item, boolean empty) {
		super.updateItem(item, empty);
		
		CheckingStatusIcon graphic = (CheckingStatusIcon)this.getGraphic();
		if (empty || item == null) {
			graphic.setVisible(false);
		} else {
			graphic.setVisible(true);
			graphic.setStatus(item);
		}
	}
}
