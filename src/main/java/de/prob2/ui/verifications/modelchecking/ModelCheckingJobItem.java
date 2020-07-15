package de.prob2.ui.verifications.modelchecking;

import de.prob.statespace.Trace;
import de.prob2.ui.verifications.Checked;

public class ModelCheckingJobItem {
	private Checked checked;

	private ModelCheckStats stats;
	
	private Trace trace;
	
	private int index;
	
	private String message;
	
	public ModelCheckingJobItem(int index, String message) {
		this.checked = Checked.NOT_CHECKED;
		this.index = index;
		this.message = message;
		this.trace = null;
	}
	
	public Checked getChecked() {
		return checked;
	}
	
	public void setChecked(final Checked checked) {
		this.checked = checked;
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
	
	public void setTrace(Trace trace) {
		this.trace = trace;
	}
	
	public Trace getTrace() {
		return trace;
	}
	
}
