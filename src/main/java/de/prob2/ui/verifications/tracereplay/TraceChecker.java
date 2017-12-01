package de.prob2.ui.verifications.tracereplay;

import java.io.File;
import java.util.List;

import com.google.common.base.Joiner;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.animator.command.GetOperationByPredicateCommand;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.check.tracereplay.PersistentTransition;
import de.prob.formula.PredicateBuilder;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.tracereplay.ReplayTrace.Status;
import javafx.application.Platform;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.collections.SetChangeListener;
import javafx.scene.control.Alert.AlertType;

@Singleton
public class TraceChecker {

	private final TraceLoader traceLoader;
	private final CurrentTrace currentTrace;
	private final StageManager stageManager;
	private final ListProperty<Thread> currentJobThreads = new SimpleListProperty<>(this, "currentJobThreads",
			FXCollections.observableArrayList());
	private ObservableMap<File, ReplayTrace> replayTraces = new SimpleMapProperty<>(this, "replayTraces",
			FXCollections.observableHashMap());

	@Inject
	private TraceChecker(final CurrentTrace currentTrace, final CurrentProject currentProject,
			final TraceLoader traceLoader, final StageManager stageManager) {
		this.currentTrace = currentTrace;
		this.traceLoader = traceLoader;
		this.stageManager = stageManager;
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
			t.setExploreStateByDefault(false);
			boolean traceReplaySuccess = true;
			try {
				for (PersistentTransition transition : trace.getStoredTrace().getTransitionList()) {
					String predicate = new PredicateBuilder().addMap(transition.getParameters())
							//TODO destination state variables are currently not supported 
							//.addMap(transition.getDestinationStateVariables())
							// TODO output parameters are currently not supported by ExecuteOperationByPredicate
							// .addMap(transition.getOuputParameters()) 
							.toString();
					final IEvalElement pred = stateSpace.getModel().parseFormula(predicate);
					final GetOperationByPredicateCommand command = new GetOperationByPredicateCommand(stateSpace,
							t.getCurrentState().getId(), transition.getOperationName(), pred, 1);
					stateSpace.execute(command);
					if (command.hasErrors()) {
						throw new IllegalArgumentException(
								"Executing operation " + transition.getOperationName() + " with predicate " + predicate
										+ " produced errors: " + Joiner.on(", ").join(command.getErrors()));
					}
					List<Transition> possibleTransitions = command.getNewTransitions();
					if (possibleTransitions.isEmpty()) {
						traceReplaySuccess = false;
					}
					t = t.add(possibleTransitions.get(0));
					if (Thread.currentThread().isInterrupted()) {
						currentJobThreads.remove(Thread.currentThread());
						return;
					}
				}
			} catch (IllegalArgumentException | de.prob.exception.ProBError e) {
				traceReplaySuccess = false;
				trace.setError(e);
			}

			trace.setStatus(traceReplaySuccess ? Status.SUCCESSFUL : Status.FAILED);
			t.setExploreStateByDefault(true);
			if (setCurrentAnimation) {
				// set the current trace in both cases
				t.getCurrentState().explore();
				currentTrace.set(t);

				if (trace.getError() != null) {
					Platform.runLater(
							() -> stageManager.makeExceptionAlert(AlertType.ERROR, "", trace.getError()).showAndWait());
				}

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

	public void resetStatus() {
		cancelReplay();
		replayTraces.forEach((file, trace) -> trace.setStatus(Status.NOT_CHECKED));
	}

	ListProperty<Thread> currentJobThreadsProperty() {
		return currentJobThreads;
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
}
