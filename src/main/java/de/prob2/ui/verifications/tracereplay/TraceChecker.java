package de.prob2.ui.verifications.tracereplay;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

import com.google.common.base.Joiner;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.be4.classicalb.core.parser.exceptions.BCompoundException;

import de.prob.animator.command.GetOperationByPredicateCommand;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.check.tracereplay.PersistentTrace;
import de.prob.check.tracereplay.PersistentTransition;
import de.prob.formula.PredicateBuilder;
import de.prob.statespace.OperationInfo;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import de.prob.translator.Translator;
import de.prob.translator.types.BObject;

import de.prob2.ui.internal.InvalidFileFormatException;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.Checked;
import javafx.application.Platform;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class TraceChecker {

	private static final Logger LOGGER = LoggerFactory.getLogger(TraceChecker.class);

	private final TraceLoader traceLoader;
	private final CurrentTrace currentTrace;
	private final CurrentProject currentProject;
	private final StageManager stageManager;
	private final ResourceBundle bundle;
	private final ListProperty<Thread> currentJobThreads = new SimpleListProperty<>(this, "currentJobThreads",
			FXCollections.observableArrayList());
	private final Map<ReplayTrace, Exception> failedTraceReplays = new HashMap<>();

	@Inject
	private TraceChecker(final CurrentTrace currentTrace, final CurrentProject currentProject,
			final TraceLoader traceLoader, final StageManager stageManager, final ResourceBundle bundle) {
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.traceLoader = traceLoader;
		this.stageManager = stageManager;
		this.bundle = bundle;
	}

	void checkAll(List<ReplayTrace> replayTraces) {
		replayTraces.forEach(trace -> replayTrace(trace, false));
		handleFailedTraceReplays();
	}

	void check(ReplayTrace replayTrace, final boolean setCurrentAnimation) {
		this.replayTrace(replayTrace, setCurrentAnimation);
		handleFailedTraceReplays();
	}

	private void handleFailedTraceReplays() {
		failedTraceReplays.forEach((trace, exception) -> {
			Path path = trace.getLocation();
			Alert alert;
			if (exception instanceof NoSuchFileException || exception instanceof FileNotFoundException) {
				alert = stageManager.makeAlert(AlertType.ERROR,
						"The trace file " + path + " could not be found.\n"
								+ "The file was probably moved, renamed or deleted.\n\n"
								+ "Would you like to remove this trace from the project?",
						ButtonType.YES, ButtonType.NO);
			} else if (exception instanceof InvalidFileFormatException) {
				alert = stageManager.makeAlert(AlertType.ERROR,
						"The file " + path + " does not contain a valid trace file.\n"
								+ "Every trace must contain a (possibly empty) transition list.\n\n"
								+ "Would you like to remove this file from the project?",
						ButtonType.YES, ButtonType.NO);
			} else {
				alert = stageManager.makeAlert(AlertType.ERROR,
						"The trace file " + path + " could not be loaded.\n\n"
								+ "Would you like to remove this trace from the project?",
						ButtonType.YES, ButtonType.NO);
			}
			alert.setHeaderText("Trace Replay Error");
			Optional<ButtonType> result = alert.showAndWait();
			if (result.isPresent() && result.get().equals(ButtonType.YES)) {
				Machine currentMachine = currentProject.getCurrentMachine();
				if (currentMachine.getTraceFiles().contains(path)) {
					currentMachine.removeTraceFile(path);
				}
			}
		});
		failedTraceReplays.clear();
	}

	private void replayTrace(ReplayTrace replayTrace, final boolean setCurrentAnimation) {
		if(!replayTrace.shouldExecute()) {
			return;
		}
		PersistentTrace persistentTrace = getPersistentTrace(replayTrace);
		if(persistentTrace == null) {
			return;
		}

		Thread replayThread = new Thread(() -> {
			replayTrace.setChecked(Checked.NOT_CHECKED);
			StateSpace stateSpace = currentTrace.getStateSpace();
			Trace trace = new Trace(stateSpace);
			trace.setExploreStateByDefault(false);
			Checked status = Checked.SUCCESS;
			final List<PersistentTransition> transitionList = persistentTrace.getTransitionList();
			for (int i = 0; i < transitionList.size(); i++) {
				final int finalI = i;
				Platform.runLater(() -> replayTrace.setProgress((double) finalI / transitionList.size()));
				Transition trans = replayPersistentTransition(replayTrace, trace, transitionList.get(i),
						setCurrentAnimation);
				if (trans != null) {
					trace = trace.add(trans);
				} else {
					status = Checked.FAIL;
					break;
				}

				if (Thread.currentThread().isInterrupted()) {
					currentJobThreads.remove(Thread.currentThread());
					return;
				}
			}

			final Checked finalStatus = status;
			Platform.runLater(() -> {
				replayTrace.setChecked(finalStatus);
				replayTrace.setProgress(-1);
			});
			trace.setExploreStateByDefault(true);
			if (setCurrentAnimation) {
				// set the current trace in both cases
				trace.getCurrentState().explore();
				currentTrace.set(trace);

				if (replayTrace.getErrorMessage() != null) {
					Platform.runLater(() -> getReplayErrorAlert(replayTrace.getErrorMessage()).showAndWait());
				}

			}
			currentJobThreads.remove(Thread.currentThread());
		}, "Trace Replay Thread");
		currentJobThreads.add(replayThread);
		replayThread.start();
	}

	private PersistentTrace getPersistentTrace(ReplayTrace replayTrace) {
		try {
			return traceLoader.loadTrace(replayTrace.getLocation());
		} catch (FileNotFoundException | NoSuchFileException e) {
			LOGGER.warn("Trace file not found", e);
			failedTraceReplays.put(replayTrace, e);
			return null;
		} catch (InvalidFileFormatException e) {
			LOGGER.warn("Invalid trace file", e);
			failedTraceReplays.put(replayTrace, e);
			return null;
		} catch (IOException e) {
			LOGGER.warn("Failed to open trace file", e);
			failedTraceReplays.put(replayTrace, e);
			return null;
		} 
	}

	private Transition replayPersistentTransition(ReplayTrace replayTrace, Trace t,
			PersistentTransition persistentTransition, boolean setCurrentAnimation) {
		StateSpace stateSpace = t.getStateSpace();
		String predicate = new PredicateBuilder().addMap(persistentTransition.getParameters()).toString();
		final IEvalElement pred = stateSpace.getModel().parseFormula(predicate, FormulaExpand.EXPAND);
		final GetOperationByPredicateCommand command = new GetOperationByPredicateCommand(stateSpace,
				t.getCurrentState().getId(), persistentTransition.getOperationName(), pred, 1);
		stateSpace.execute(command);
		if (command.hasErrors()) {
			String errorMessage = String.format(bundle.getString("verifications.tracereplay.errorMessage"),
					persistentTransition.getOperationName(), predicate, Joiner.on(", ").join(command.getErrors()));
			replayTrace.setErrorMessage(errorMessage);
			return null;
		}
		List<Transition> possibleTransitions = command.getNewTransitions();
		if (possibleTransitions.isEmpty()) {
			String errorMessage = String.format(
					bundle.getString("verifications.tracereplay.errorMessage.operationNotPossible"),
					persistentTransition.getOperationName(), predicate);
			replayTrace.setErrorMessage(errorMessage);
			return null;
		}
		Transition trans = possibleTransitions.get(0);
		if (!checkOutputParams(replayTrace, trans, persistentTransition, setCurrentAnimation)) {
			return null;
		}
		return trans;
	}

	private boolean checkOutputParams(ReplayTrace replayTrace, Transition trans,
			PersistentTransition persistentTransition, boolean setCurrentAnimation) {
		String operationName = trans.getName();
		OperationInfo machineOperationInfo = trans.stateSpace.getLoadedMachine().getMachineOperationInfo(operationName);
		final Map<String, String> ouputParameters = persistentTransition.getOuputParameters();
		if (machineOperationInfo != null && ouputParameters != null) {
			List<String> outputParameterNames = machineOperationInfo.getOutputParameterNames();
			try {
				List<BObject> translatedReturnValues = trans.getTranslatedReturnValues();
				for (int i = 0; i < outputParameterNames.size(); i++) {
					String outputParamName = outputParameterNames.get(i);
					BObject paramValueFromTransition = translatedReturnValues.get(i);
					if (ouputParameters.containsKey(outputParamName)) {
						String stringValue = ouputParameters.get(outputParamName);
						BObject bValue = Translator.translate(stringValue);
						if (!bValue.equals(paramValueFromTransition)) {
							// do we need further checks here?
							// because the value translator does not
							// support enum values properly
							if (setCurrentAnimation) {
								String errorMessage = String.format(
										bundle.getString(
												"verifications.tracereplay.errorMessage.mismatchingOutputValues"),
										operationName, outputParamName, bValue.toString(), paramValueFromTransition);
								replayTrace.setErrorMessage(errorMessage);
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

	public ListProperty<Thread> currentJobThreadsProperty() {
		return currentJobThreads;
	}
}
