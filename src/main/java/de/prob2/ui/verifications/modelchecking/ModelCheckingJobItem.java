package de.prob2.ui.verifications.modelchecking;

import de.prob.check.IModelCheckingResult;
import de.prob.check.ModelCheckGoalFound;
import de.prob.check.ModelCheckOk;
import de.prob.check.StateSpaceStats;
import de.prob.statespace.ITraceDescription;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import de.prob2.ui.verifications.Checked;

public class ModelCheckingJobItem {
	private final int index;
	private final IModelCheckingResult result;
	private final Checked checked;
	private final long timeElapsed;
	private final StateSpaceStats stats;
	private final StateSpace stateSpace;
	private Trace trace;
	
	public ModelCheckingJobItem(final int index, final IModelCheckingResult result, final long timeElapsed, final StateSpaceStats stats, final StateSpace stateSpace) {
		this.index = index;
		this.result = result;
		if (result instanceof ModelCheckOk || result instanceof ModelCheckGoalFound) {
			this.checked = Checked.SUCCESS;
		} else if (result instanceof ITraceDescription) {
			this.checked = Checked.FAIL;
		} else {
			this.checked = Checked.TIMEOUT;
		}
		
		this.timeElapsed = timeElapsed;
		this.stats = stats;
		this.stateSpace = stateSpace;
		this.trace = null;
	}
	
	public int getIndex() {
		return index;
	}
	
	public IModelCheckingResult getResult() {
		return result;
	}
	
	public Checked getChecked() {
		return checked;
	}
	
	public String getMessage() {
		return this.getResult().getMessage();
	}
	
	public long getTimeElapsed() {
		return timeElapsed;
	}
	
	public StateSpaceStats getStats() {
		return stats;
	}
	
	public StateSpace getStateSpace() {
		return stateSpace;
	}
	
	public Trace getTrace() {
		if (this.trace == null && this.getResult() instanceof ITraceDescription) {
			this.trace = ((ITraceDescription)this.getResult()).getTrace(this.getStateSpace());
		}
		
		return trace;
	}
}
