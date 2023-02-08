package de.prob2.ui.verifications;

import javafx.scene.control.TableCell;

public final class CheckedCell<T> extends TableCell<T, Checked> {
	public CheckedCell() {
		super();
		
		this.setText(null);
		final CheckedIcon iconView = new CheckedIcon();
		iconView.setVisible(false);
		this.setGraphic(iconView);
	}
	
	@Override
	protected void updateItem(final Checked item, final boolean empty) {
		super.updateItem(item, empty);
		
		final CheckedIcon graphic = (CheckedIcon)this.getGraphic();
		if (empty || item == null) {
			graphic.setVisible(false);
		} else {
			graphic.setVisible(true);
			graphic.setChecked(item);
		}
	}
}
