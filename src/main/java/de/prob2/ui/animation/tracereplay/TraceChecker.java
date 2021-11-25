package de.prob2.ui.animation.tracereplay;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Scanner;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.animator.command.ReplayTraceFileCommand;
import de.prob.check.tracereplay.OperationDisabledness;
import de.prob.check.tracereplay.OperationEnabledness;
import de.prob.check.tracereplay.PersistentTrace;
import de.prob.check.tracereplay.PersistentTransition;
import de.prob.check.tracereplay.Postcondition;
import de.prob.check.tracereplay.PostconditionPredicate;
import de.prob.check.tracereplay.TraceReplay;
import de.prob.check.tracereplay.TransitionReplayPrecision;
import de.prob.exception.ProBError;
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
import javafx.scene.control.Alert;

@FXMLInjected
@Singleton
public class TraceChecker {

	private final CurrentTrace currentTrace;
	private final Injector injector;
	private final StageManager stageManager;
	private final ResourceBundle bundle;
	private final ListProperty<Thread> currentJobThreads = new SimpleListProperty<>(this, "currentJobThreads",
			FXCollections.observableArrayList());

	@Inject
	private TraceChecker(final CurrentTrace currentTrace,  final Injector injector, final StageManager stageManager,
						 final DisablePropertyController disablePropertyController, final ResourceBundle bundle) {
		this.currentTrace = currentTrace;
		this.injector = injector;
		this.stageManager = stageManager;
		this.bundle = bundle;
		disablePropertyController.addDisableExpression(this.runningProperty());
	}

	public void checkAll(List<ReplayTrace> replayTraces) {
		replayTraces.forEach(trace -> replayTrace(trace, false));
	}

	public void check(ReplayTrace replayTrace, final boolean setCurrentAnimation) {
		replayTrace(replayTrace, setCurrentAnimation);
	}

	public void check(ReplayTrace replayTrace, final boolean setCurrentAnimation, IAfterTraceReplay afterTraceReplay) {
		replayTrace(replayTrace, setCurrentAnimation, afterTraceReplay);
	}

	private void replayTrace(ReplayTrace replayTrace, final boolean setCurrentAnimation) {
		replayTrace(replayTrace, setCurrentAnimation, () -> {});
	}

	private void replayTrace(ReplayTrace replayTrace, final boolean setCurrentAnimation, IAfterTraceReplay afterTraceReplay) {
		if(!replayTrace.selected()) {
			return;
		}
		replayTrace.setChecked(Checked.NOT_CHECKED);
		replayTrace.setReplayedTrace(null);
		replayTrace.setReplayError(null);
		// ReplayTraceFileCommand doesn't support progress updates yet,
		// so set an indeterminate status for now.
		// We cannot use -1, because it is already used to say that no replay is currently running
		// (this is special-cased in TraceViewHandler).
		replayTrace.setProgress(-2);
		StateSpace stateSpace = currentTrace.getStateSpace();
		final Path traceFile = injector.getInstance(CurrentProject.class).getLocation().resolve(replayTrace.getLocation());
		Thread replayThread = new Thread(() -> {
			try {
				final ReplayTraceFileCommand cmd = new ReplayTraceFileCommand(traceFile.toString());
				try {
					stateSpace.execute(cmd);
					Platform.runLater(() -> replayTrace.setChecked(Checked.SUCCESS));
				} catch (ProBError e) {
					replayTrace.setReplayError(e);
					Platform.runLater(() -> replayTrace.setChecked(Checked.FAIL));
				}
				replayTrace.setReplayedTrace(cmd.getTrace());
				Trace trace = cmd.getTrace().getTrace(stateSpace);
				final PersistentTrace persistentTrace = replayTrace.getPersistentTrace();
				final List<List<TraceReplay.PostconditionResult>> postconditionResults = TraceReplay.checkPostconditionsAfterReplay(persistentTrace, trace);
				storePostconditionResults(replayTrace, postconditionResults);
				showTestError(persistentTrace, postconditionResults);
				if (setCurrentAnimation) {
					// set the current trace if no error has occurred. Otherwise leave the decision to the user
					if (replayTrace.getReplayError() != null) {
						showTraceReplayCompleteFailed(trace, replayTrace);
					} else {
						currentTrace.set(trace);
					}
					afterTraceReplay.apply();
				}
			} finally {
				Platform.runLater(() -> replayTrace.setProgress(-1));
				currentJobThreads.remove(Thread.currentThread());
			}
		}, "Trace Replay Thread");
		currentJobThreads.add(replayThread);
		replayThread.start();
	}

	public void showTestError(PersistentTrace persistentTrace, List<List<TraceReplay.PostconditionResult>> postconditionResults) {
		assert persistentTrace.getTransitionList().size() >= postconditionResults.size();
		StringBuilder sb = new StringBuilder();
		List<PersistentTransition> transitions = persistentTrace.getTransitionList();
		boolean failed = false;
		for(int i = 0; i < postconditionResults.size(); i++) {
			PersistentTransition transition = transitions.get(i);
			List<TraceReplay.PostconditionResult> postconditionTransitionResults = postconditionResults.get(i);
			for(int j = 0; j < postconditionTransitionResults.size(); j++) {
				TraceReplay.PostconditionResult result = postconditionTransitionResults.get(j);
				if(result != TraceReplay.PostconditionResult.SUCCESS) {
					Postcondition postcondition = transition.getPostconditions().get(j);
					switch (postcondition.getKind()) {
						case PREDICATE:
							sb.append(String.format(bundle.getString("animation.trace.replay.test.alert.content.predicate"), transition.getOperationName(), ((PostconditionPredicate) postcondition).getPredicate()));
							if(result == TraceReplay.PostconditionResult.PARSE_ERROR) {
								sb.append(bundle.getString("animation.trace.replay.test.alert.content.parseError"));
							}
							sb.append("\n");
							break;
						case ENABLEDNESS: {
							String predicate = ((OperationEnabledness) postcondition).getPredicate();
							if (predicate.isEmpty()) {
								sb.append(String.format(bundle.getString("animation.trace.replay.test.alert.content.enabled"), transition.getOperationName(), ((OperationEnabledness) postcondition).getOperation()));
							} else {
								sb.append(String.format(bundle.getString("animation.trace.replay.test.alert.content.enabledWithPredicate"), transition.getOperationName(), ((OperationEnabledness) postcondition).getOperation(), predicate));
							}
							if(result == TraceReplay.PostconditionResult.PARSE_ERROR) {
								sb.append(bundle.getString("animation.trace.replay.test.alert.content.parseError"));
							}
							sb.append("\n");
							break;
						}
						case DISABLEDNESS: {
							String predicate = ((OperationDisabledness) postcondition).getPredicate();
							if (predicate.isEmpty()) {
								sb.append(String.format(bundle.getString("animation.trace.replay.test.alert.content.disabled"), transition.getOperationName(), ((OperationDisabledness) postcondition).getOperation()));
							} else {
								sb.append(String.format(bundle.getString("animation.trace.replay.test.alert.content.disabledWithPredicate"), transition.getOperationName(), ((OperationDisabledness) postcondition).getOperation(), predicate));
							}
							if(result == TraceReplay.PostconditionResult.PARSE_ERROR) {
								sb.append(bundle.getString("animation.trace.replay.test.alert.content.parseError"));
							}
							sb.append("\n");
							break;
						}
						default:
							throw new RuntimeException("Postcondition kind is unknown: " + postcondition.getKind());
					}
					failed = true;
					sb.append("\n");
				}
			}
		}
		if(failed) {
			Platform.runLater(() -> {
				Alert alert = new Alert(Alert.AlertType.ERROR);
				alert.setHeaderText(bundle.getString("animation.trace.replay.test.alert.header"));
				alert.setContentText(sb.toString());
				stageManager.register(alert);
				alert.showAndWait();
			});
		}
	}

	private static void storePostconditionResults(final ReplayTrace replayTrace, final List<List<TraceReplay.PostconditionResult>> postconditionResults) {
		final List<List<Checked>> convertedResults = new ArrayList<>();
		boolean successful = true;
		for (List<TraceReplay.PostconditionResult> transitionResults : postconditionResults) {
			final List<Checked> convertedTransitionResults = new ArrayList<>();
			for (TraceReplay.PostconditionResult result : transitionResults) {
				final Checked convertedResult;
				if (result == TraceReplay.PostconditionResult.SUCCESS) {
					convertedResult = Checked.SUCCESS;
				} else if (result == TraceReplay.PostconditionResult.FAIL) {
					convertedResult = Checked.FAIL;
					successful = false;
				} else {
					convertedResult = Checked.PARSE_ERROR;
					successful = false;
				}
				convertedTransitionResults.add(convertedResult);
			}
			convertedResults.add(convertedTransitionResults);
		}
		replayTrace.setPostconditionStatus(convertedResults);
		if (!successful) {
			Platform.runLater(() -> replayTrace.setChecked(Checked.FAIL));
		}
	}

	private void showTraceReplayCompleteFailed(Trace trace, final ReplayTrace replayTrace) {
		PersistentTrace persistentTrace = replayTrace.getPersistentTrace();
		Platform.runLater(() -> {
			// TODO Implement displaying rich error information in TraceReplayErrorAlert (using ErrorTableView) instead of converting the error messages to a string
			TraceReplayErrorAlert alert = new TraceReplayErrorAlert(injector, "common.literal", TraceReplayErrorAlert.Trigger.TRIGGER_TRACE_CHECKER, replayTrace.getReplayError().getMessage());

			stageManager.register(alert);
			alert.setLineNumber(lineNumber(replayTrace, trace.size()));
			alert.setAttemptedReplayOrLostTrace(trace);
			alert.setStoredTrace(persistentTrace);
			alert.setHistory(currentTrace.get());
			currentTrace.set(trace);
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

	private int lineNumber(ReplayTrace replayTrace, int failedTraceLength) {
		final List<TransitionReplayPrecision> precisions = replayTrace.getReplayedTrace().getTransitionReplayPrecisions();
		int firstTransitionWithError = failedTraceLength;
		for (int i = 0; i < precisions.size(); i++) {
			if (precisions.get(i) != TransitionReplayPrecision.PRECISE) {
				firstTransitionWithError = i;
				break;
			}
		}
		final List<PersistentTransition> transitionList = replayTrace.getPersistentTrace().getTransitionList();
		if (firstTransitionWithError >= transitionList.size()) {
			// Every transition was replayed, and each one was precise, but we still got a replay error...
			// We have no other way to figure out which transition failed, so just give up.
			return -1;
		}
		PersistentTransition failedTransition = transitionList.get(firstTransitionWithError);
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
					if (nextLine.contains(failedTransition.getOperationName()) && operationNumber > firstTransitionWithError) {
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
