package de.prob2.ui.sharedviews;

import de.prob2.ui.layout.BindableGlyph;
import de.prob2.ui.verifications.Checked;

import javafx.scene.paint.Color;

import org.controlsfx.glyphfont.FontAwesome;

public final class TraceViewHandler {
	private TraceViewHandler() {
		throw new AssertionError("Utility class");
	}

	public static void updateStatusIcon(final BindableGlyph iconView, final Checked status) {
		switch (status) {
			case SUCCESS:
				iconView.setIcon(FontAwesome.Glyph.CHECK);
				iconView.setTextFill(Color.GREEN);
				break;

			case FAIL:
				iconView.setIcon(FontAwesome.Glyph.REMOVE);
				iconView.setTextFill(Color.RED);
				break;

			case NOT_CHECKED:
				iconView.setIcon(FontAwesome.Glyph.QUESTION_CIRCLE);
				iconView.setTextFill(Color.BLUE);
				break;
			case PARSE_ERROR:
				iconView.setIcon(FontAwesome.Glyph.WARNING);
				iconView.setTextFill(Color.ORANGE);
				break;
			default:
				throw new AssertionError("Unhandled status: " + status);
		}
	}
}
