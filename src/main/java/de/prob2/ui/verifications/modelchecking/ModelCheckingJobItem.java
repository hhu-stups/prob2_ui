package de.prob2.ui.verifications.modelchecking;

import de.prob.statespace.Trace;
import de.prob2.ui.verifications.Checked;

public class ModelCheckingJobItem {
	private final int index;
	private final Checked checked;
	private final String message;
	private final ModelCheckStats stats;
	private final Trace trace;
	
	public ModelCheckingJobItem(final int index, final Checked checked, final String message, final ModelCheckStats stats, final Trace trace) {
		this.index = index;
		this.checked = checked;
		this.message = message;
		this.stats = stats;
		this.trace = trace;
	}
	
	public int getIndex() {
		return index;
	}
	
	public Checked getChecked() {
		return checked;
	}
	
	public String getMessage() {
		return message;
	}
	
	public ModelCheckStats getStats() {
		return stats;
	}
	
	public Trace getTrace() {
		return trace;
	}
}
