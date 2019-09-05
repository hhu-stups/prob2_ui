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
		iconView.getStyleClass().addAll("boolean-icon", "true");
		this.setGraphic(iconView);
	}
	
	@Override
	protected void updateItem(final Boolean item, final boolean empty) {
		super.updateItem(item, empty);
		
		final BindableGlyph graphic = (BindableGlyph)this.getGraphic();
		graphic.getStyleClass().removeAll("true", "false");
		if (empty || item == null) {
			graphic.setVisible(false);
		} else {
			graphic.setVisible(true);
			final String styleClass;
			final FontAwesome.Glyph icon;
			if (item) {
				styleClass = "true";
				icon = FontAwesome.Glyph.CHECK;
			} else {
				styleClass = "false";
				icon = FontAwesome.Glyph.REMOVE;
			}
			graphic.getStyleClass().add(styleClass);
			graphic.setIcon(icon);
		}
	}
}
