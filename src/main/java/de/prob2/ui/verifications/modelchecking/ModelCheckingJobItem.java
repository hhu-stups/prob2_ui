package de.prob2.ui.verifications.modelchecking;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import de.prob.statespace.Trace;
import de.prob2.ui.verifications.Checked;
import javafx.scene.paint.Color;

public class ModelCheckingJobItem {

	private transient ModelCheckStats stats;
	
	private transient FontAwesomeIconView status;
	
	private transient Trace trace;
	
	private Checked checked;
	
	private int index;
	
	private String message;
	
	public ModelCheckingJobItem(int index, String message) {
		this.index = index;
		this.message = message;
		
		this.status = new FontAwesomeIconView(FontAwesomeIcon.QUESTION_CIRCLE);
		this.status.setFill(Color.BLUE);
		this.checked = Checked.NOT_CHECKED;
		this.trace = null;
	}
	
	public void setStats(ModelCheckStats stats) {
		this.stats = stats;
	}
	
	public ModelCheckStats getStats() {
		return stats;
	}
	
	public int getIndex() {
		return index;
	}
	
	public String getMessage() {
		return message;
	}
	
	public FontAwesomeIconView getStatus() {
		return status;
	}
	
	public void setCheckedSuccessful() {
		FontAwesomeIconView icon = new FontAwesomeIconView(FontAwesomeIcon.CHECK);
		icon.setFill(Color.GREEN);
		this.status = icon;
		this.checked = Checked.SUCCESS;
	}

	public void setCheckedFailed() {
		FontAwesomeIconView icon = new FontAwesomeIconView(FontAwesomeIcon.REMOVE);
		icon.setFill(Color.RED);
		this.status = icon;
		this.checked = Checked.FAIL;
	}
	
	public void setTimeout() {
		FontAwesomeIconView icon = new FontAwesomeIconView(FontAwesomeIcon.EXCLAMATION_TRIANGLE);
		icon.setFill(Color.YELLOW);
		this.status = icon;
		this.checked = Checked.TIMEOUT;
	}
	
	public Checked getChecked() {
		return checked;
	}
	
	public void setTrace(Trace trace) {
		this.trace = trace;
	}
	
	public Trace getTrace() {
		return trace;
	}
	
}
