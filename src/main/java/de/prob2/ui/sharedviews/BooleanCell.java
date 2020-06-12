package de.prob2.ui.sharedviews;

import de.prob2.ui.layout.BindableGlyph;

import javafx.scene.control.TableCell;

import org.controlsfx.glyphfont.FontAwesome;

public final class BooleanCell<T> extends TableCell<T, Boolean> {
	public BooleanCell() {
		super();
		
		this.setText(null);
		final BindableGlyph iconView = new BindableGlyph("FontAwesome", FontAwesome.Glyph.CHECK);
		iconView.setVisible(false);
		this.setGraphic(iconView);
	}
	
	@Override
	protected void updateItem(final Boolean item, final boolean empty) {
		super.updateItem(item, empty);
		
		this.getGraphic().setVisible(!empty && item != null && item);
	}
}
