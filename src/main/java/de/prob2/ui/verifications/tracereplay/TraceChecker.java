package de.prob2.ui.verifications.tracereplay;

import java.nio.file.Path;
import java.util.List;

import com.google.common.base.Joiner;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.be4.classicalb.core.parser.exceptions.BCompoundException;

import de.prob.animator.command.GetMachineOperationInfos.OperationInfo;
import de.prob.animator.command.GetOperationByPredicateCommand;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.check.tracereplay.PersistentTransition;
import de.prob.formula.PredicateBuilder;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import de.prob.translator.Translator;
import de.prob.translator.types.BObject;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;

import javafx.application.Platform;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.collections.SetChangeListener;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

@Singleton
public class TraceChecker {

	private final TraceLoader traceLoader;
	private final CurrentTrace currentTrace;
	private final StageManager stageManager;
	private final ListProperty<Thread> currentJobThreads = new SimpleListProperty<>(this, "currentJobThreads",
			FXCollections.observableArrayList());
	private ObservableMap<Path, ReplayTrace> replayTraces = new SimpleMapProperty<>(this, "replayTraces",
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
		replayTraces.forEach((path, trace) -> replayTrace(trace, false));
	}

	public void replayTrace(Path traceFile, final boolean setCurrentAnimation) {
		replayTrace(replayTraces.get(traceFile), setCurrentAnimation);
	}

	public void replayTrace(ReplayTrace replayTrace, final boolean setCurrentAnimation) {
		if(replayTrace.getStoredTrace() == null) {
			return;
		}
		
		Thread replayThread = new Thread(() -> {
			replayTrace.setStatus(ReplayTrace.Status.NOT_CHECKED);

			StateSpace stateSpace = currentTrace.getStateSpace();

			Trace trace = new Trace(stateSpace);
			trace.setExploreStateByDefault(false);
			ReplayTrace.Status status = ReplayTrace.Status.SUCCESSFUL;
			final List<PersistentTransition> transitionList = replayTrace.getStoredTrace().getTransitionList();
			for (int i = 0; i < transitionList.size(); i++) {
				final int finalI = i;
				Platform.runLater(() -> replayTrace.setProgress(finalI));
				Transition trans = replayPersistentTransition(stateSpace, trace, transitionList.get(i), setCurrentAnimation);
				if (trans != null) {
					trace = trace.add(trans);
				} else {
					status = ReplayTrace.Status.FAILED;
					break;
				}

				if (Thread.currentThread().isInterrupted()) {
					currentJobThreads.remove(Thread.currentThread());
					return;
				}
			}

			final ReplayTrace.Status finalStatus = status;
			Platform.runLater(() -> {
				replayTrace.setStatus(finalStatus);
				replayTrace.setProgress(-1);
			});
			trace.setExploreStateByDefault(true);
			if (setCurrentAnimation) {
				// set the current trace in both cases
				trace.getCurrentState().explore();
				currentTrace.set(trace);

				if (replayTrace.getError() != null) {
					Platform.runLater(() -> stageManager.makeExceptionAlert("", replayTrace.getError()).showAndWait());
				}

			}
			currentJobThreads.remove(Thread.currentThread());
		}, "Trace Replay Thread");
		currentJobThreads.add(replayThread);
		replayThread.start();
	}

	private Transition replayPersistentTransition(StateSpace stateSpace, Trace t,
			PersistentTransition persistentTransition, boolean setCurrentAnimation) {
		String predicate = new PredicateBuilder().addMap(persistentTransition.getParameters())
				.toString();
		final IEvalElement pred = stateSpace.getModel().parseFormula(predicate);
		final GetOperationByPredicateCommand command = new GetOperationByPredicateCommand(stateSpace,
				t.getCurrentState().getId(), persistentTransition.getOperationName(), pred, 1);
		stateSpace.execute(command);
		if (command.hasErrors()) {
			String errorMessage = "Executing operation " + persistentTransition.getOperationName() + " with predicate "
					+ predicate + " produced errors: " + Joiner.on(", ").join(command.getErrors());
			Platform.runLater(() -> getReplayErrorAlert(errorMessage).showAndWait());
			return null;
		}
		List<Transition> possibleTransitions = command.getNewTransitions();
		if (possibleTransitions.isEmpty()) {
			String errorMessage = "Executing operation " + persistentTransition.getOperationName() + " with predicate "
					+ predicate + " produced errors: Operation not possible.";
			Platform.runLater(() -> getReplayErrorAlert(errorMessage).showAndWait());
			return null;
		}
		Transition trans = possibleTransitions.get(0);
		if (!checkOutputParams(stateSpace, trans, persistentTransition, setCurrentAnimation)) {
			return null;
		}
		return trans;
	}

	private boolean checkOutputParams(StateSpace stateSpace, Transition trans,
			PersistentTransition persistentTransition, boolean setCurrentAnimation) {
		String operationName = trans.getName();
		OperationInfo machineOperationInfo = stateSpace.getLoadedMachine().getMachineOperationInfo(operationName);
		if (machineOperationInfo != null) {
			List<String> outputParameterNames = machineOperationInfo.getOutputParameterNames();
			try {
				List<BObject> translatedReturnValues = trans.getTranslatedReturnValues();
				for (int i = 0; i < outputParameterNames.size(); i++) {
					String outputParamName = outputParameterNames.get(i);
					BObject paramValueFromTransition = translatedReturnValues.get(i);
					if (persistentTransition.getOuputParameters().containsKey(outputParamName)) {
						String stringValue = persistentTransition.getOuputParameters().get(outputParamName);
						BObject bValue = Translator.translate(stringValue);
						if (!bValue.equals(paramValueFromTransition)) {
							// do we need further checks here?
							// because the value translator does not
							// support enum values properly
							if (setCurrentAnimation) {
								String errorMessage = String.format(
										"Can not replay operation '%s'. The value of the ouput parameter '%s' does not match.\n"
												+ "Value from trace file: %s\nComputed value: %s",
										operationName, outputParamName, bValue.toString(), paramValueFromTransition);
								Platform.runLater(() ->
									Platform.runLater(() -> getReplayErrorAlert(errorMessage).showAndWait())
								);
							}
							return false;
						}
					}

				}
			} catch (BCompoundException e) {
				Platform.runLater(() -> stageManager.makeExceptionAlert("", e.getFirstException()).showAndWait());
				return false;
			}
		}
		return true;
	}

	private Alert getReplayErrorAlert(String errorMessage) {
		Alert alert = stageManager.makeAlert(AlertType.ERROR, errorMessage);
		alert.setHeaderText("Replay Error");
		return alert;
	}

	void cancelReplay() {
		currentJobThreads.forEach(Thread::interrupt);
		currentJobThreads.clear();
	}

	public void resetStatus() {
		cancelReplay();
		replayTraces.forEach((path, trace) -> trace.setStatus(ReplayTrace.Status.NOT_CHECKED));
	}

	ListProperty<Thread> currentJobThreadsProperty() {
		return currentJobThreads;
	}

	private void addTrace(Path traceFile) {
		ReplayTrace trace = traceLoader.loadTrace(traceFile);
		replayTraces.put(traceFile, trace);
	}

	private void removeTrace(Path traceFile) {
		replayTraces.remove(traceFile);
	}

	ObservableMap<Path, ReplayTrace> getReplayTraces() {
		return replayTraces;
	}

	private void updateReplayTraces(Machine machine) {
		replayTraces.clear();
		if (machine != null) {
			machine.getTraceFiles().forEach(this::addTrace);
			machine.getTraceFiles().addListener((SetChangeListener<Path>) c -> {
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
