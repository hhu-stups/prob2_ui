package de.prob2.ui.verifications.modelchecking;

import de.prob.statespace.ITraceDescription;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import de.prob2.ui.verifications.Checked;

public class ModelCheckingJobItem {
	private final int index;
	private final Checked checked;
	private final String message;
	private final ModelCheckStats stats;
	private final StateSpace stateSpace;
	private final ITraceDescription traceDescription;
	private Trace trace;
	
	public ModelCheckingJobItem(final int index, final Checked checked, final String message, final ModelCheckStats stats, final StateSpace stateSpace, final ITraceDescription traceDescription) {
		this.index = index;
		this.checked = checked;
		this.message = message;
		this.stats = stats;
		this.stateSpace = stateSpace;
		this.traceDescription = traceDescription;
		this.trace = null;
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
	
	public StateSpace getStateSpace() {
		return stateSpace;
	}
	
	public ITraceDescription getTraceDescription() {
		return traceDescription;
	}
	
	public Trace getTrace() {
		if (this.trace == null && this.getTraceDescription() != null) {
			this.trace = this.getTraceDescription().getTrace(this.getStateSpace());
		}
		
		return trace;
	}
}
