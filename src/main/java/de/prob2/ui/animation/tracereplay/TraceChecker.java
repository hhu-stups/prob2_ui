package de.prob2.ui.animation.tracereplay;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.animator.domainobjects.ErrorItem;
import de.prob.check.tracereplay.OperationDisabledness;
import de.prob.check.tracereplay.OperationEnabledness;
import de.prob.check.tracereplay.PersistentTrace;
import de.prob.check.tracereplay.PersistentTransition;
import de.prob.check.tracereplay.Postcondition;
import de.prob.check.tracereplay.PostconditionPredicate;
import de.prob.check.tracereplay.ReplayedTrace;
import de.prob.check.tracereplay.TraceReplay;
import de.prob.check.tracereplay.TraceReplayStatus;
import de.prob.check.tracereplay.TransitionReplayPrecision;
import de.prob.check.tracereplay.json.storage.TraceJsonFile;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import de.prob2.ui.internal.DisablePropertyController;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.executor.CliTaskExecutor;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.verifications.Checked;

import javafx.application.Platform;
import javafx.beans.binding.BooleanExpression;
import javafx.scene.control.Alert;

@FXMLInjected
@Singleton
public class TraceChecker {
	private final CliTaskExecutor cliExecutor;
	private final CurrentTrace currentTrace;
	private final Injector injector;
	private final StageManager stageManager;
	private final I18n i18n;

	@Inject
	private TraceChecker(final CliTaskExecutor cliExecutor, final CurrentTrace currentTrace,  final Injector injector, final StageManager stageManager,
						 final DisablePropertyController disablePropertyController, final I18n i18n) {
		this.cliExecutor = cliExecutor;
		this.currentTrace = currentTrace;
		this.injector = injector;
		this.stageManager = stageManager;
		this.i18n = i18n;
		disablePropertyController.addDisableExpression(this.runningProperty());
	}

	public void checkAll(List<ReplayTrace> replayTraces) {
		replayTraces.forEach(trace -> check(trace, false));
	}

	public void check(ReplayTrace replayTrace, final boolean setCurrentAnimation) {
		check(replayTrace, setCurrentAnimation, () -> {});
	}

	public void check(ReplayTrace replayTrace, final boolean setCurrentAnimation, IAfterTraceReplay afterTraceReplay) {
		if(!replayTrace.selected()) {
			return;
		}

		replayTrace.reset();
		// ReplayTraceFileCommand doesn't support progress updates yet,
		// so set an indeterminate status for now.
		// We cannot use -1, because it is already used to say that no replay is currently running
		// (this is special-cased in TraceViewHandler).
		replayTrace.setProgress(-2);
		StateSpace stateSpace = currentTrace.getStateSpace();
		final CompletableFuture<ReplayTrace> future = cliExecutor.submit(() -> {
			final TraceJsonFile traceJsonFile = replayTrace.load();
			ReplayedTrace replayed = TraceReplay.replayTraceFile(stateSpace, replayTrace.getAbsoluteLocation());
			if (replayed.getErrors().isEmpty() && replayed.getReplayStatus() == TraceReplayStatus.PARTIAL) {
				// FIXME Should this case be reported as an error on the Prolog side?
				final ErrorItem error = new ErrorItem("Trace could not be replayed completely", ErrorItem.Type.ERROR, Collections.emptyList());
				replayed = replayed.withErrors(Collections.singletonList(error));
			}
			final ReplayedTrace replayedFinal = replayed;
			replayTrace.setReplayedTrace(replayed);
			// TODO Display replay information for each transition if the replay was not perfect/complete
			Platform.runLater(() -> replayTrace.setChecked(replayedFinal.getErrors().isEmpty() ? Checked.SUCCESS : Checked.FAIL));
			Trace trace = replayed.getTrace(stateSpace);
			replayTrace.setAnimatedReplayedTrace(trace);
			final PersistentTrace persistentTrace = new PersistentTrace(traceJsonFile.getDescription(), traceJsonFile.getTransitionList());
			final List<List<TraceReplay.PostconditionResult>> postconditionResults = TraceReplay.checkPostconditionsAfterReplay(persistentTrace, trace);
			storePostconditionResults(replayTrace, postconditionResults);
			showTestError(traceJsonFile.getTransitionList(), replayTrace.getPostconditionStatus());
			return replayTrace;
		});
		future.whenComplete((r, e) -> {
			Platform.runLater(() -> replayTrace.setProgress(-1));
			if (e == null) {
				if (setCurrentAnimation) {
					// set the current trace if no error has occurred. Otherwise leave the decision to the user
					if (!r.getReplayedTrace().getErrors().isEmpty()) {
						showTraceReplayCompleteFailed(replayTrace);
					} else {
						currentTrace.set(r.getAnimatedReplayedTrace());
					}
					afterTraceReplay.apply();
				}
			} else {
				Platform.runLater(() -> replayTrace.setChecked(Checked.PARSE_ERROR));
				injector.getInstance(TraceFileHandler.class).showLoadError(r.getAbsoluteLocation(), e);
			}
		});
	}

	public void showTestError(List<PersistentTransition> transitions, List<List<Checked>> postconditionResults) {
		assert transitions.size() >= postconditionResults.size();
		StringBuilder sb = new StringBuilder();
		boolean failed = false;
		for(int i = 0; i < postconditionResults.size(); i++) {
			PersistentTransition transition = transitions.get(i);
			List<Checked> postconditionTransitionResults = postconditionResults.get(i);
			for(int j = 0; j < postconditionTransitionResults.size(); j++) {
				Checked result = postconditionTransitionResults.get(j);
				if(result != Checked.SUCCESS) {
					assert result == Checked.FAIL || result == Checked.PARSE_ERROR;
					Postcondition postcondition = transition.getPostconditions().get(j);
					switch (postcondition.getKind()) {
						case PREDICATE:
							sb.append(i18n.translate("animation.trace.replay.test.alert.content.predicate", transition.getOperationName(), ((PostconditionPredicate) postcondition).getPredicate()));
							if(result == Checked.PARSE_ERROR) {
								sb.append(i18n.translate("animation.trace.replay.test.alert.content.parseError"));
							}
							sb.append("\n");
							break;
						case ENABLEDNESS: {
							String predicate = ((OperationEnabledness) postcondition).getPredicate();
							if (predicate.isEmpty()) {
								sb.append(i18n.translate("animation.trace.replay.test.alert.content.enabled", transition.getOperationName(), ((OperationEnabledness) postcondition).getOperation()));
							} else {
								sb.append(i18n.translate("animation.trace.replay.test.alert.content.enabledWithPredicate", transition.getOperationName(), ((OperationEnabledness) postcondition).getOperation(), predicate));
							}
							if(result == Checked.PARSE_ERROR) {
								sb.append(i18n.translate("animation.trace.replay.test.alert.content.parseError"));
							}
							sb.append("\n");
							break;
						}
						case DISABLEDNESS: {
							String predicate = ((OperationDisabledness) postcondition).getPredicate();
							if (predicate.isEmpty()) {
								sb.append(i18n.translate("animation.trace.replay.test.alert.content.disabled", transition.getOperationName(), ((OperationDisabledness) postcondition).getOperation()));
							} else {
								sb.append(i18n.translate("animation.trace.replay.test.alert.content.disabledWithPredicate", transition.getOperationName(), ((OperationDisabledness) postcondition).getOperation(), predicate));
							}
							if(result == Checked.PARSE_ERROR) {
								sb.append(i18n.translate("animation.trace.replay.test.alert.content.parseError"));
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
				alert.setHeaderText(i18n.translate("animation.trace.replay.test.alert.header"));
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

	private void showTraceReplayCompleteFailed(final ReplayTrace replayTrace) {
		final Trace trace = replayTrace.getAnimatedReplayedTrace();
		Platform.runLater(() -> {
			// TODO Implement displaying rich error information in TraceReplayErrorAlert (using ErrorTableView) instead of converting the error messages to a string
			final String errorMessage = replayTrace.getReplayedTrace().getErrors().stream()
				.map(ErrorItem::toString)
				.collect(Collectors.joining("\n"));
			TraceReplayErrorAlert alert = new TraceReplayErrorAlert(injector, "common.literal", TraceReplayErrorAlert.Trigger.TRIGGER_TRACE_CHECKER, errorMessage);

			stageManager.register(alert);
			alert.setLineNumber(lineNumber(replayTrace, trace.size()));
			alert.setAttemptedReplayOrLostTrace(trace);
			alert.setStoredTrace(replayTrace.getLoadedTrace());
			alert.setHistory(currentTrace.get());
			currentTrace.set(trace);
			alert.setErrorMessage();
		});
	}

	public void cancelReplay() {
		cliExecutor.interruptAll();
	}

	public BooleanExpression runningProperty() {
		return cliExecutor.runningProperty();
	}

	public boolean isRunning() {
		return this.runningProperty().get();
	}

	private static int lineNumber(ReplayTrace replayTrace, int failedTraceLength) {
		final List<TransitionReplayPrecision> precisions = replayTrace.getReplayedTrace().getTransitionReplayPrecisions();
		int firstTransitionWithError = failedTraceLength;
		for (int i = 0; i < precisions.size(); i++) {
			if (precisions.get(i) != TransitionReplayPrecision.PRECISE) {
				firstTransitionWithError = i;
				break;
			}
		}
		final List<PersistentTransition> transitionList = replayTrace.getLoadedTrace().getTransitionList();
		if (firstTransitionWithError >= transitionList.size()) {
			// Every transition was replayed, and each one was precise, but we still got a replay error...
			// We have no other way to figure out which transition failed, so just give up.
			return -1;
		}
		PersistentTransition failedTransition = transitionList.get(firstTransitionWithError);
		int lineNumber = 0;
		int operationNumber = 0;
		try {
			Scanner scanner = new Scanner(replayTrace.getAbsoluteLocation());
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
