package de.prob2.ui.animation.tracereplay;


import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import de.prob.animator.command.GetOperationByPredicateCommand;
import de.prob.check.tracereplay.ITraceChecker;
import de.prob.check.tracereplay.PersistentTrace;
import de.prob.check.tracereplay.PersistentTransition;
import de.prob.check.tracereplay.TraceReplay;
import de.prob.formula.PredicateBuilder;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.tracediff.TraceDiffStage;
import de.prob2.ui.verifications.Checked;
import javafx.application.Platform;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.Region;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Scanner;

@FXMLInjected
@Singleton
public class TraceChecker implements ITraceChecker {

	private final CurrentTrace currentTrace;
	private final Injector injector;
	private final StageManager stageManager;
	private final ListProperty<Thread> currentJobThreads = new SimpleListProperty<>(this, "currentJobThreads",
			FXCollections.observableArrayList());
	private boolean isNewTrace = false;

	@Inject
	private TraceChecker(final CurrentTrace currentTrace, final Injector injector, final StageManager stageManager) {
		this.currentTrace = currentTrace;
		this.injector = injector;
		this.stageManager = stageManager;
	}

	void checkAll(List<ReplayTrace> replayTraces) {
		replayTraces.forEach(trace -> replayTrace(trace, false));
	}

	void check(ReplayTrace replayTrace, final boolean setCurrentAnimation) {
		this.replayTrace(replayTrace, setCurrentAnimation);
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
				// set the current trace if no error has occured. Otherwise leave the decision to the user
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
                replayTrace.setErrorMessageParams(persistentTransition.getOperationName(), predicateBuilder, String.join(", ", command.getErrors()));
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
		final Trace copyTrace = trace;
		if (isNewTrace) {
			Platform.runLater(() -> {
				Alert alert = new Alert(Alert.AlertType.WARNING);
				alert.initOwner(stageManager.getCurrent());
				alert.setContentText(String.format(injector.getInstance(ResourceBundle.class).getString("animation.tracereplay.alerts.traceReplayError.newTraceContent"), lineNumber(replayTrace), replayTrace.getErrorMessageParams()[0].toString(), replayTrace.getErrorMessageParams()[1].toString()));
				alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
				alert.show();
			});
			isNewTrace = false;
		} else {
			PersistentTrace persistentTrace = replayTrace.getPersistentTrace();
			Platform.runLater(() -> {
				TraceReplayErrorAlert alert = new TraceReplayErrorAlert(injector, replayTrace.getErrorMessageBundleKey(), replayTrace.getErrorMessageParams());
				alert.initOwner(stageManager.getCurrent());
				boolean isEqual = currentTrace.get().getTransitionList().equals(copyTrace.getTransitionList());
				alert.setErrorMessage(false, isEqual, copyTrace.getTransitionList().size(), persistentTrace.getTransitionList().size());
				setAlertButtons(isEqual, copyTrace, persistentTrace, alert);
			});
		}
	}

	private void setAlertButtons(boolean isEqual, Trace copyTrace, PersistentTrace persistentTrace, Alert alert) {
		if (isEqual) {
			alert.getButtonTypes().addAll(ButtonType.OK);
			alert.showAndWait();
		} else {
			ButtonType traceDiffButton = new ButtonType(injector.getInstance(ResourceBundle.class).getString("animation.tracereplay.alerts.traceReplayError.error.traceDiff"));
			alert.getButtonTypes().addAll(traceDiffButton, ButtonType.YES, ButtonType.NO);
			handleAlert(alert, copyTrace, persistentTrace, traceDiffButton);
		}
	}

	public void handleAlert(Alert alert, Trace copyTrace, PersistentTrace persistentTrace, ButtonType traceDiffButton) {
		injector.getInstance(TraceDiffStage.class).close();
		Optional<ButtonType> type = alert.showAndWait();
		if (type.get() == ButtonType.YES) {
			currentTrace.set(copyTrace);
		} else if (type.get() == traceDiffButton) {
			TraceDiffStage traceDiffStage = injector.getInstance(TraceDiffStage.class);
			traceDiffStage.setAlert(alert);
			traceDiffStage.setLists(copyTrace, persistentTrace, currentTrace.get());
			traceDiffStage.show();
			alert.close();
		}
	}

	void cancelReplay() {
		currentJobThreads.forEach(Thread::interrupt);
		currentJobThreads.clear();
	}

	public ListProperty<Thread> currentJobThreadsProperty() {
		return currentJobThreads;
	}

	void isNewTrace() {
		isNewTrace = true;
	}

	private int lineNumber(ReplayTrace replayTrace) {
		int lineNumber = 1;
		try {
			Scanner scanner = new Scanner(injector.getInstance(CurrentProject.class).getLocation().resolve(replayTrace.getLocation()));
			while (scanner.hasNext()) {
				// Error messages should have already been set at this point so there is no possibility of a NPE
				if(scanner.nextLine().contains(replayTrace.getErrorMessageParams()[0].toString())) {
					break;
				}
				lineNumber++;
			}
		} catch (IOException e) {
			// Not possible at this position since a persistent trace already exists
		}
		return lineNumber;
	}
}
