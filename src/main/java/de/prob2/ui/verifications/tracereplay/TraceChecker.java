package de.prob2.ui.verifications.tracereplay;

import com.google.inject.Inject;

import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.verifications.tracereplay.ReplayTrace.Status;
import javafx.collections.ObservableList;

public class TraceChecker {
	
	private final CurrentTrace currentTrace;

	@Inject
	private TraceChecker(final CurrentTrace currentTrace) {
		this.currentTrace = currentTrace;
	}
	
	void checkMachine(ObservableList<ReplayTraceItem> traceItems) {
		traceItems.forEach(traceItem -> replayTrace(traceItem.getTrace()));
	}

	private void replayTrace(ReplayTrace trace) {
		trace.setStatus(Status.NOT_CHECKED);

		StateSpace stateSpace = currentTrace.getStateSpace();
		Trace t = new Trace(stateSpace);

		try {
			for (ReplayTransition transition : trace.getTransitionList()) {
				t = t.addTransitionWith(transition.getName(), transition.getParameters());
			}
		} catch (IllegalArgumentException e) {
			trace.setStatus(Status.FAILED);
			return;
		}
		trace.setStatus(Status.SUCCESSFUL);
	}
}
