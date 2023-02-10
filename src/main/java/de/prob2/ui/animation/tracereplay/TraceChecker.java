package de.prob2.ui.animation.tracereplay;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.animator.domainobjects.ErrorItem;
import de.prob.check.tracereplay.PersistentTransition;
import de.prob.check.tracereplay.ReplayedTrace;
import de.prob.check.tracereplay.TraceReplay;
import de.prob.check.tracereplay.TraceReplayStatus;
import de.prob.check.tracereplay.TransitionReplayPrecision;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.executor.CliTaskExecutor;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.verifications.Checked;

import javafx.application.Platform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@FXMLInjected
@Singleton
public class TraceChecker {

	private static final Logger LOGGER = LoggerFactory.getLogger(TraceChecker.class);

	private final CliTaskExecutor cliExecutor;
	private final CurrentTrace currentTrace;
	private final Injector injector;
	private final StageManager stageManager;

	@Inject
	private TraceChecker(final CliTaskExecutor cliExecutor, final CurrentTrace currentTrace, final Injector injector, final StageManager stageManager) {
		this.cliExecutor = cliExecutor;
		this.currentTrace = currentTrace;
		this.injector = injector;
		this.stageManager = stageManager;
	}

	public void checkAll(List<ReplayTrace> replayTraces) {
		replayTraces.stream()
			.filter(ReplayTrace::selected)
			.forEach(trace -> check(trace, false));
	}

	public CompletableFuture<ReplayTrace> check(ReplayTrace replayTrace, final boolean setCurrentAnimation) {
		return checkNoninteractive(replayTrace).whenComplete((r, e) -> {
			if (e == null) {
				if (setCurrentAnimation) {
					// set the current trace if no error has occurred. Otherwise leave the decision to the user
					if (!r.getReplayedTrace().getErrors().isEmpty()) {
						showTraceReplayCompleteFailed(replayTrace);
					} else {
						currentTrace.set(r.getAnimatedReplayedTrace());
					}
				}
			} else {
				Platform.runLater(() -> injector.getInstance(TraceFileHandler.class).showLoadError(replayTrace.getAbsoluteLocation(), e));
			}
		});
	}

	public CompletableFuture<ReplayTrace> checkNoninteractive(ReplayTrace replayTrace) {
		replayTrace.reset();
		// ReplayTraceFileCommand doesn't support progress updates yet,
		// so set an indeterminate status for now.
		// We cannot use -1, because it is already used to say that no replay is currently running
		// (this is special-cased in TraceReplayView).
		replayTrace.setProgress(-2);
		StateSpace stateSpace = currentTrace.getStateSpace();
		final CompletableFuture<ReplayTrace> future = cliExecutor.submit(() -> {
			ReplayedTrace replayed = TraceReplay.replayTraceFile(stateSpace, replayTrace.getAbsoluteLocation());
			List<ErrorItem> errors = replayed.getErrors();
			if (errors.isEmpty() && replayed.getReplayStatus() != TraceReplayStatus.PERFECT) {
				// FIXME Should this case be reported as an error on the Prolog side?
				final ErrorItem error = new ErrorItem("Trace could not be replayed completely", ErrorItem.Type.ERROR, Collections.emptyList());
				errors = new ArrayList<>(errors);
				errors.add(error);
			}
			replayed = replayed.withErrors(errors);
			replayTrace.setReplayedTrace(replayed);
			Trace trace = replayed.getTrace(stateSpace);
			replayTrace.setAnimatedReplayedTrace(trace);
			return replayTrace;
		});
		return future.whenComplete((r, e) -> {
			final Checked res;
			if (e == null) {
				if (replayTrace.getReplayedTrace().getErrors().isEmpty()) {
					res = Checked.SUCCESS;
				} else {
					res = Checked.FAIL;
				}
			} else if (e instanceof CancellationException) {
				// Trace check was interrupted by user
				res = Checked.NOT_CHECKED;
			} else {
				res = Checked.PARSE_ERROR;
			}
			replayTrace.setChecked(res);
			Platform.runLater(() -> replayTrace.setProgress(-1));
		});
	}

	private void showTraceReplayCompleteFailed(final ReplayTrace replayTrace) {
		final Trace trace = replayTrace.getAnimatedReplayedTrace();
		Platform.runLater(() -> {
			// TODO: switch this to ReplayedTraceStatusAlert once ready
			final String errorMessage = replayTrace.getReplayedTrace().getErrors().stream()
					                            .map(ErrorItem::toString)
					                            .collect(Collectors.joining("\n"));
			TraceReplayErrorAlert alert = new TraceReplayErrorAlert(injector, "common.literal", TraceReplayErrorAlert.Trigger.TRIGGER_TRACE_CHECKER, errorMessage);

			stageManager.register(alert);
			try {
				alert.setLineNumber(lineNumber(replayTrace, trace.size()));
			} catch (IOException e) {
				LOGGER.error("error while trying to read trace file after the replay failed", e);
			}
			alert.setReplayTrace(replayTrace);
			alert.setAttemptedReplayOrLostTrace(trace);
			alert.setStoredTrace(replayTrace.getLoadedTrace());
			alert.setHistory(currentTrace.get());
			currentTrace.set(trace);
			alert.setErrorMessage();
		});
	}

	@Deprecated
	private static int lineNumber(ReplayTrace replayTrace, int failedTraceLength) throws IOException {
		final List<TransitionReplayPrecision> precisions = replayTrace.getReplayedTrace().getTransitionReplayPrecisions();
		int firstTransitionWithError = failedTraceLength;
		for (int i = 0; i < precisions.size(); i++) {
			if (precisions.get(i) != TransitionReplayPrecision.PRECISE) {
				firstTransitionWithError = i;
				break;
			}
		}

		replayTrace.load();
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
