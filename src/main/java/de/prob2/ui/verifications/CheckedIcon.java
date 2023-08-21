package de.prob2.ui.verifications;

import de.prob2.ui.layout.BindableGlyph;

import org.controlsfx.glyphfont.FontAwesome;

public final class CheckedIcon extends BindableGlyph {
	public CheckedIcon() {
		super("FontAwesome", FontAwesome.Glyph.QUESTION_CIRCLE);
		
		this.getStyleClass().addAll("checked-icon", "not-checked");
	}
	
	public void setChecked(final Checked checked) {
		this.getStyleClass().removeAll("not-checked", "success", "fail", "interrupted", "timeout", "parse-error");
		final String styleClass;
		final FontAwesome.Glyph icon;
		switch (checked) {
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
			
			case INTERRUPTED:
				styleClass = "interrupted";
				icon = FontAwesome.Glyph.PAUSE;
				break;
			
			case TIMEOUT:
				styleClass = "timeout";
				icon = FontAwesome.Glyph.CLOCK_ALT;
				break;
			
			case PARSE_ERROR:
				styleClass = "parse-error";
				icon = FontAwesome.Glyph.EXCLAMATION_TRIANGLE;
				break;
			
			default:
				throw new IllegalArgumentException("Unknown checking status: " + checked);
		}
		this.getStyleClass().add(styleClass);
		this.setIcon(icon);
	}
}
