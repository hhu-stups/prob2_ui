package de.prob2.ui.verifications;

import javafx.scene.control.TableCell;

public final class CheckingStatusCell<T> extends TableCell<T, CheckingStatus> {
	public CheckingStatusCell() {
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
