package de.prob2.ui.animation.tracereplay;


import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import de.prob.animator.command.GetOperationByPredicateCommand;
import de.prob.check.tracereplay.ITraceChecker;
import de.prob.check.tracereplay.PersistentTrace;
import de.prob.check.tracereplay.PersistentTransition;
import de.prob.check.tracereplay.TraceReplay;
import de.prob.check.tracereplay.json.storage.TraceJsonFile;
import de.prob.check.tracereplay.json.storage.TraceMetaData;
import de.prob.formula.PredicateBuilder;
import de.prob.scripting.ModelTranslationError;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import de.prob2.ui.internal.DisablePropertyController;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.verifications.Checked;
import javafx.application.Platform;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;

@FXMLInjected
@Singleton
public class TraceChecker implements ITraceChecker {

	private final CurrentTrace currentTrace;
	private final Injector injector;
	private final StageManager stageManager;
	private final ListProperty<Thread> currentJobThreads = new SimpleListProperty<>(this, "currentJobThreads",
			FXCollections.observableArrayList());
	private final CurrentProject currentProject;

	@Inject
	private TraceChecker(final CurrentTrace currentTrace,  final CurrentProject currentProject,
						 final Injector injector, final StageManager stageManager,
						 final DisablePropertyController disablePropertyController) {
		this.currentTrace = currentTrace;
		this.injector = injector;
		this.stageManager = stageManager;
		this.currentProject = currentProject;
		disablePropertyController.addDisableExpression(this.runningProperty());
	}

	public void checkAll(List<ReplayTrace> replayTraces) {
		replayTraces.forEach(trace -> replayTrace(trace, false));
	}

	public void check(ReplayTrace replayTrace, final boolean setCurrentAnimation) {
		replayTrace(replayTrace, setCurrentAnimation);
	}

	private void replayTrace(ReplayTrace replayTrace, final boolean setCurrentAnimation) {
		if(!replayTrace.selected()) {
			return;
		}
		PersistentTrace persistentTrace = replayTrace.getPersistentTrace();
		if(persistentTrace == null) {
			return;
		}
		replayTrace.setChecked(Checked.NOT_CHECKED);
		StateSpace stateSpace = currentTrace.getStateSpace();
		Map<String, Object> replayInformation = new HashMap<>();
		replayInformation.put("replayTrace", replayTrace);
		Thread replayThread = new Thread(() -> {

			Trace trace = TraceReplay.replayTrace(persistentTrace, stateSpace, setCurrentAnimation, replayInformation, this);
			if (setCurrentAnimation) {
				// set the current trace if no error has occurred. Otherwise leave the decision to the user
				if (replayTrace.getErrorMessageBundleKey() != null) {
					showTraceReplayCompleteFailed(trace, replayInformation);
				} else {
					currentTrace.set(trace);
				}
			}
			currentJobThreads.remove(Thread.currentThread());
		}, "Trace Replay Thread");
		currentJobThreads.add(replayThread);
		replayThread.start();
	}

	public void updateProgress(double value, Map<String, Object> replayInformation) {
		ReplayTrace replayTrace = (ReplayTrace) replayInformation.get("replayTrace");
		Platform.runLater(() -> replayTrace.setProgress(value));
	}

	public void setResult(boolean success, Map<String, Object> replayInformation) {
		ReplayTrace replayTrace = (ReplayTrace) replayInformation.get("replayTrace");
		Platform.runLater(() -> {
			if(success) {
				replayTrace.setChecked(Checked.SUCCESS);
			} else {
				replayTrace.setChecked(Checked.FAIL);
			}
			replayTrace.setProgress(-1);
		});
	}

	public void afterInterrupt() {
		currentJobThreads.remove(Thread.currentThread());
	}

	public void showError(TraceReplay.TraceReplayError errorType, Map<String, Object> replayInformation) {
		switch(errorType) {
			case COMMAND: {
				ReplayTrace replayTrace = (ReplayTrace) replayInformation.get("replayTrace");
				GetOperationByPredicateCommand command = (GetOperationByPredicateCommand) replayInformation.get("command");
				PersistentTransition persistentTransition = (PersistentTransition) replayInformation.get("persistentTransition");
				PredicateBuilder predicateBuilder = (PredicateBuilder) replayInformation.get("predicateBuilder");
				replayTrace.setErrorMessageBundleKey("animation.tracereplay.traceChecker.errorMessage");
				replayTrace.setErrorMessageParams(persistentTransition.getOperationName(), predicateBuilder, command.getErrors().stream().map(GetOperationByPredicateCommand.GetOperationError::getMessage).collect(Collectors.joining(", ")));
				break;
			}
			case NO_OPERATION_POSSIBLE: {
				ReplayTrace replayTrace = (ReplayTrace) replayInformation.get("replayTrace");
				PersistentTransition persistentTransition = (PersistentTransition) replayInformation.get("persistentTransition");
				PredicateBuilder predicateBuilder = (PredicateBuilder) replayInformation.get("predicateBuilder");
				replayTrace.setErrorMessageBundleKey("animation.tracereplay.traceChecker.errorMessage.operationNotPossible");
				replayTrace.setErrorMessageParams(persistentTransition.getOperationName(), predicateBuilder);
				break;
			}
			case MISMATCH_OUTPUT: {
				ReplayTrace replayTrace = (ReplayTrace) replayInformation.get("replayTrace");
				String operationName = (String) replayInformation.get("operationName");
				String outputParamName = (String) replayInformation.get("outputParamName");
				String bValue = (String) replayInformation.get("bValue");
				String paramValueFromTransition = (String) replayInformation.get("paramValue");
				replayTrace.setErrorMessageBundleKey("animation.tracereplay.traceChecker.errorMessage.mismatchingOutputValues");
				replayTrace.setErrorMessageParams(operationName, outputParamName, bValue, paramValueFromTransition);
				break;
			}
			case TRACE_REPLAY: {
				Exception e = (Exception) replayInformation.get("exception");
				Platform.runLater(
						() -> stageManager
								.makeExceptionAlert(e,
										"animation.tracereplay.alerts.traceReplayError.header",
										"animation.tracereplay.alerts.traceReplayError.content")
								.showAndWait());
				break;
			}
			default:
				break;
		}
	}



	private void showTraceReplayCompleteFailed(Trace trace, Map<String, Object> replayInformation) {
		ReplayTrace replayTrace = (ReplayTrace) replayInformation.get("replayTrace");
		final Trace copyFailedTrace = trace;
		PersistentTrace persistentTrace = replayTrace.getPersistentTrace();
		Platform.runLater(() -> {
			TraceReplayErrorAlert alert = new TraceReplayErrorAlert(injector, replayTrace.getErrorMessageBundleKey(), TraceReplayErrorAlert.Trigger.TRIGGER_TRACE_CHECKER, replayTrace.getErrorMessageParams());

			stageManager.register(alert);
			alert.setLineNumber(lineNumber(replayTrace, copyFailedTrace.size()));
			alert.setAttemptedReplayOrLostTrace(copyFailedTrace);
			alert.setStoredTrace(persistentTrace);
			alert.setHistory(currentTrace.get());
			currentTrace.set(copyFailedTrace);
			alert.setErrorMessage();
		});
	}

	public void cancelReplay() {
		currentJobThreads.forEach(Thread::interrupt);
		currentJobThreads.clear();
	}

	public BooleanExpression runningProperty() {
		return currentJobThreads.emptyProperty().not();
	}

	public boolean isRunning() {
		return this.runningProperty().get();
	}

	private int lineNumber(ReplayTrace replayTrace, int copyFailedTraceLength) {
		PersistentTransition failedTransition = replayTrace.getPersistentTrace().getTransitionList().get(copyFailedTraceLength);
		int lineNumber = 0;
		int operationNumber = 0;
		try {
			Scanner scanner = new Scanner(injector.getInstance(CurrentProject.class).getLocation().resolve(replayTrace.getLocation()));
			while (scanner.hasNext()) {
				// Error messages should have already been set at this point so there is no possibility of a NPE
				lineNumber++;
				String nextLine = scanner.nextLine();
				if (nextLine.contains("name")) {
					operationNumber++;
					if (nextLine.contains(failedTransition.getOperationName()) && operationNumber > copyFailedTraceLength) {
						break;
					}
				}
			}
		} catch (IOException e) {
			// Not possible at this position since a persistent trace already exists
		}
		return lineNumber;
	}

}
