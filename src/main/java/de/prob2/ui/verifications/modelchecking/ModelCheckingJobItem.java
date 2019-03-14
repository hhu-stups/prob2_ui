package de.prob2.ui.verifications.modelchecking;

import de.prob.statespace.Trace;

public class ModelCheckingJobItem extends AbstractModelCheckingItem {

	private transient ModelCheckStats stats;
	
	private transient Trace trace;
	
	private int index;
	
	private String message;
	
	public ModelCheckingJobItem(int index, String message) {
		super();
		this.index = index;
		this.message = message;
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
	
	public void setTrace(Trace trace) {
		this.trace = trace;
	}
	
	public Trace getTrace() {
		return trace;
	}
	
}
