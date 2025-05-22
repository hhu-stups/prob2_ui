package de.prob2.ui.verifications;

import de.prob2.ui.layout.BindableGlyph;

import org.controlsfx.glyphfont.FontAwesome;

public final class CheckingStatusIcon extends BindableGlyph {
	public CheckingStatusIcon() {
		super("FontAwesome", FontAwesome.Glyph.QUESTION_CIRCLE);
		
		this.getStyleClass().addAll("checking-status-icon", "not-checked");
	}
	
	public void setStatus(CheckingStatus status) {
		this.getStyleClass().removeAll("not-checked", "in-progress", "success", "fail", "interrupted", "timeout", "invalid-task");
		final String styleClass;
		final FontAwesome.Glyph icon = switch (status) {
			case NOT_CHECKED -> {
				styleClass = "not-checked";
				yield FontAwesome.Glyph.QUESTION_CIRCLE;
			}
			case IN_PROGRESS -> {
				// TODO Display a real ProgressIndicator here instead of a spinner icon that doesn't spin
				styleClass = "in-progress";
				yield FontAwesome.Glyph.SPINNER;
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
			default -> throw new IllegalArgumentException("Unknown checking status: " + status);
		};
		this.getStyleClass().add(styleClass);
		this.setIcon(icon);
	}
}
