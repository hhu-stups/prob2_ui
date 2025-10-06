package de.prob2.ui.verifications.modelchecking;

import java.math.BigInteger;

import de.prob.check.IModelCheckingResult;
import de.prob.check.ModelCheckGoalFound;
import de.prob.check.ModelCheckOk;
import de.prob.check.NotYetFinished;
import de.prob.check.StateSpaceStats;
import de.prob.statespace.ITraceDescription;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import de.prob2.ui.verifications.CheckingStatus;

public class ModelCheckingStep {
	private final IModelCheckingResult result;
	private final CheckingStatus status;
	private final long timeElapsed;
	private final StateSpaceStats stats;
	private final BigInteger memoryUsed;
	private final StateSpace stateSpace;
	private Trace trace;
	
	public ModelCheckingStep(final IModelCheckingResult result, final long timeElapsed, final StateSpaceStats stats, final BigInteger memoryUsed, final StateSpace stateSpace) {
		this.result = result;
		if (result instanceof NotYetFinished) {
			this.status = CheckingStatus.IN_PROGRESS;
		} else if (result instanceof ModelCheckOk || result instanceof ModelCheckGoalFound) {
			this.status = CheckingStatus.SUCCESS;
		} else if (result instanceof ITraceDescription) {
			this.status = CheckingStatus.FAIL;
		} else {
			this.status = CheckingStatus.TIMEOUT;
		}
		
		this.timeElapsed = timeElapsed;
		this.stats = stats;
		this.memoryUsed = memoryUsed;
		this.stateSpace = stateSpace;
		this.trace = null;
	}
	
	public IModelCheckingResult getResult() {
		return result;
	}
	
	public CheckingStatus getStatus() {
		return status;
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
	
	public BigInteger getMemoryUsed() {
		return this.memoryUsed;
	}
	
	public StateSpace getStateSpace() {
		return stateSpace;
	}
	
	public boolean hasTrace() {
		return this.trace != null || (this.getResult() instanceof ITraceDescription traceDescription && traceDescription.hasTrace());
	}
	
	public Trace getTrace() {
		if (this.trace == null && this.getResult() instanceof ITraceDescription traceDescription && traceDescription.hasTrace()) {
			this.trace = traceDescription.getTrace(this.getStateSpace());
		}
		
		return trace;
	}
}
