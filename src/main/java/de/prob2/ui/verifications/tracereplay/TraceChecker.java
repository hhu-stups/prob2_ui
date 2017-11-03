package de.prob2.ui.verifications.tracereplay;

import com.google.inject.Inject;

import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.verifications.tracereplay.ReplayTrace.Status;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class TraceChecker {

	private final CurrentTrace currentTrace;
	private final ListProperty<Thread> currentJobThreads = new SimpleListProperty<>(this, "currentJobThreads", FXCollections.observableArrayList());

	@Inject
	private TraceChecker(final CurrentTrace currentTrace) {
		this.currentTrace = currentTrace;
	}

	void checkMachine(ObservableList<ReplayTraceItem> traceItems) {
		traceItems.forEach(traceItem -> replayTrace(traceItem.getTrace()));
	}

	private void replayTrace(ReplayTrace trace) {
		Thread replayThread = new Thread(() -> {
			trace.setStatus(Status.NOT_CHECKED);

			StateSpace stateSpace = currentTrace.getStateSpace();
			Trace t = new Trace(stateSpace);

			try {
				for (ReplayTransition transition : trace.getTransitionList()) {
					t = t.addTransitionWith(transition.getName(), transition.getParameters());
					if(Thread.currentThread().isInterrupted()) {
						currentJobThreads.remove(Thread.currentThread());
						return;
					}
				}
			} catch (IllegalArgumentException e) {
				trace.setStatus(Status.FAILED);
				currentJobThreads.remove(Thread.currentThread());
				return;
			}
			trace.setStatus(Status.SUCCESSFUL);
			currentJobThreads.remove(Thread.currentThread());
		});
		currentJobThreads.add(replayThread);
		replayThread.start();
	}

	void cancelReplay() {
		currentJobThreads.forEach(Thread::interrupt);
		currentJobThreads.clear();
	}
	
	ListProperty<Thread> currentJobThreadsProperty() {
		return currentJobThreads;
	}
}
