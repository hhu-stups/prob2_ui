package de.prob2.ui.verifications.modelchecking;

import de.prob2.ui.layout.BindableGlyph;
import de.prob2.ui.verifications.Checked;

import javafx.scene.paint.Color;

import org.controlsfx.glyphfont.FontAwesome;

public abstract class AbstractModelCheckingItem {

	protected transient BindableGlyph status;

	protected Checked checked;

	public AbstractModelCheckingItem() {
		this.status = new BindableGlyph("FontAwesome", FontAwesome.Glyph.QUESTION_CIRCLE);
		this.status.setTextFill(Color.BLUE);
		this.checked = Checked.NOT_CHECKED;
	}

	public BindableGlyph getStatus() {
		return status;
	}

	public Checked getChecked() {
		return checked;
	}

	public void setCheckedSuccessful() {
		this.status.setIcon(FontAwesome.Glyph.CHECK);
		this.status.setTextFill(Color.GREEN);
		this.checked = Checked.SUCCESS;
	}

	public void setCheckedFailed() {
		this.status.setIcon(FontAwesome.Glyph.REMOVE);
		this.status.setTextFill(Color.RED);
		this.checked = Checked.FAIL;
	}

	public void setTimeout() {
		this.status.setIcon(FontAwesome.Glyph.EXCLAMATION_TRIANGLE);
		this.status.setTextFill(Color.YELLOW);
		this.checked = Checked.TIMEOUT;
	}

}
