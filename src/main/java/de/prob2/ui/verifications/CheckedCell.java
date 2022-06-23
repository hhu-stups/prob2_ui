package de.prob2.ui.verifications;

import de.prob2.ui.layout.BindableGlyph;

import javafx.scene.control.TableCell;

import org.controlsfx.glyphfont.FontAwesome;

public final class CheckedCell<T> extends TableCell<T, Checked> {
	public CheckedCell() {
		super();
		
		this.setText(null);
		final BindableGlyph iconView = new BindableGlyph("FontAwesome", FontAwesome.Glyph.QUESTION_CIRCLE);
		iconView.setVisible(false);
		iconView.getStyleClass().addAll("checked-icon", "not-checked");
		this.setGraphic(iconView);
	}
	
	@Override
	protected void updateItem(final Checked item, final boolean empty) {
		super.updateItem(item, empty);
		
		final BindableGlyph graphic = (BindableGlyph)this.getGraphic();
		graphic.getStyleClass().removeAll("not-checked", "success", "fail", "interrupted", "parse-error");
		if (empty || item == null) {
			graphic.setVisible(false);
		} else {
			graphic.setVisible(true);
			final String styleClass;
			final FontAwesome.Glyph icon;
			switch (item) {
				case UNKNOWN:
				case NOT_CHECKED:
					styleClass = "not-checked";
					icon = FontAwesome.Glyph.QUESTION_CIRCLE;
					break;
				
				case SUCCESS:
					styleClass = "success";
					icon = FontAwesome.Glyph.CHECK;
					break;
				
				case FAIL:
					styleClass = "fail";
					icon = FontAwesome.Glyph.REMOVE;
					break;
				case LIMIT_REACHED:
				case INTERRUPTED:
				case TIMEOUT:
					styleClass = "interrupted";
					icon = FontAwesome.Glyph.EXCLAMATION_TRIANGLE;
					break;
				
				case PARSE_ERROR:
					styleClass = "parse-error";
					icon = FontAwesome.Glyph.EXCLAMATION_TRIANGLE;
					break;
				
				default:
					throw new IllegalArgumentException("Unknown checking status: " + item);
			}
			graphic.getStyleClass().add(styleClass);
			graphic.setIcon(icon);
		}
	}
}
