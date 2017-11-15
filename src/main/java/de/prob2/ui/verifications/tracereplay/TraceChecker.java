package de.prob2.ui.verifications.tracereplay;

import java.io.File;

import com.google.inject.Inject;

import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.tracereplay.ReplayTrace.Status;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.collections.SetChangeListener;

public class TraceChecker {

	private ObservableMap<File, ReplayTrace> replayTraces = new SimpleMapProperty<>(this, "replayTraces", FXCollections.observableHashMap());

	private final TraceLoader traceLoader;
	private final CurrentTrace currentTrace;
	private final ListProperty<Thread> currentJobThreads = new SimpleListProperty<>(this, "currentJobThreads",
			FXCollections.observableArrayList());

	@Inject
	private TraceChecker(final CurrentTrace currentTrace, final CurrentProject currentProject,
			final TraceLoader traceLoader) {
		this.currentTrace = currentTrace;
		this.traceLoader = traceLoader;
		currentProject.currentMachineProperty().addListener((observable, from, to) -> updateReplayTraces(to));
	}

	void checkMachine() {
		replayTraces.forEach((file, trace) -> replayTrace(trace, false));
	}

	public void replayTrace(File traceFile, final boolean setCurrentAnimation) {
		replayTrace(replayTraces.get(traceFile), setCurrentAnimation);
	}

	public void replayTrace(ReplayTrace trace, final boolean setCurrentAnimation) {
		Thread replayThread = new Thread(() -> {
			trace.setStatus(Status.NOT_CHECKED);

			StateSpace stateSpace = currentTrace.getStateSpace();
			Trace t = new Trace(stateSpace);
			boolean traceReplaySuccess = true;
			try {
				for (ReplayTransition transition : trace.getTransitionList()) {
					t = t.addTransitionWith(transition.getName(), transition.getParameters());
					if (Thread.currentThread().isInterrupted()) {
						currentJobThreads.remove(Thread.currentThread());
						return;
					}
				}
			} catch (IllegalArgumentException | de.prob.exception.ProBError e) {
				traceReplaySuccess = false;
				// TODO display warning
			}
			if (traceReplaySuccess) {
				trace.setStatus(Status.SUCCESSFUL);
			} else {
				trace.setStatus(Status.FAILED);
			}
			if (setCurrentAnimation) {
				// set the current trace in both cases
				currentTrace.set(t);
			}
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

	private void updateReplayTraces(Machine machine) {
		replayTraces.clear();
		if (machine != null) {
			for (File traceFile : machine.getTraceFiles()) {
				addTrace(traceFile);
			}
			machine.getTraceFiles().addListener((SetChangeListener<File>) c -> {
				if (c.wasAdded()) {
					addTrace(c.getElementAdded());
				}
				if (c.wasRemoved()) {
					removeTrace(c.getElementRemoved());
				}
			});
		}
	}

	private void addTrace(File traceFile) {
		ReplayTrace trace = traceLoader.loadTrace(traceFile);
		replayTraces.put(traceFile, trace);
	}

	private void removeTrace(File traceFile) {
		replayTraces.remove(traceFile);
	}

	ObservableMap<File, ReplayTrace> getReplayTraces() {
		return replayTraces;
	}
}
