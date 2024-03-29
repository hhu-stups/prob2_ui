package de.prob2.ui.verifications;

import de.prob2.ui.layout.BindableGlyph;

import org.controlsfx.glyphfont.FontAwesome;

public final class CheckedIcon extends BindableGlyph {
	public CheckedIcon() {
		super("FontAwesome", FontAwesome.Glyph.QUESTION_CIRCLE);
		
		this.getStyleClass().addAll("checked-icon", "not-checked");
	}
	
	public void setChecked(final Checked checked) {
		this.getStyleClass().removeAll("not-checked", "success", "fail", "interrupted", "timeout", "invalid-task");
		final String styleClass;
		final FontAwesome.Glyph icon = switch (checked) {
			case NOT_CHECKED -> {
				styleClass = "not-checked";
				yield FontAwesome.Glyph.QUESTION_CIRCLE;
			}
			case SUCCESS -> {
				styleClass = "success";
				yield FontAwesome.Glyph.CHECK;
			}
			case FAIL -> {
				styleClass = "fail";
				yield FontAwesome.Glyph.REMOVE;
			}
			case INTERRUPTED -> {
				styleClass = "interrupted";
				yield FontAwesome.Glyph.PAUSE;
			}
			case TIMEOUT -> {
				styleClass = "timeout";
				yield FontAwesome.Glyph.CLOCK_ALT;
			}
			case INVALID_TASK -> {
				styleClass = "invalid-task";
				yield FontAwesome.Glyph.EXCLAMATION_TRIANGLE;
			}
			default -> throw new IllegalArgumentException("Unknown checking status: " + checked);
		};
		this.getStyleClass().add(styleClass);
		this.setIcon(icon);
	}
}
