package de.prob2.ui.animation.tracereplay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import de.prob.animator.CommandInterruptedException;
import de.prob.animator.domainobjects.ErrorItem;
import de.prob.check.tracereplay.ReplayedTrace;
import de.prob.check.tracereplay.TraceReplay;
import de.prob.check.tracereplay.TraceReplayStatus;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.verifications.CheckingResult;
import de.prob2.ui.verifications.CheckingStatus;

import javafx.application.Platform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@FXMLInjected
@Singleton
public final class TraceChecker {
	private static final Logger LOGGER = LoggerFactory.getLogger(TraceChecker.class);

	private final CurrentTrace currentTrace;
	private final Provider<ReplayedTraceStatusAlert> replayedAlertProvider;

	@Inject
	private TraceChecker(CurrentTrace currentTrace, Provider<ReplayedTraceStatusAlert> replayedAlertProvider) {
		this.currentTrace = currentTrace;
		this.replayedAlertProvider = replayedAlertProvider;
	}

	private static void checkNoninteractiveInternal(ReplayTrace replayTrace, StateSpace stateSpace) {
		ReplayedTrace replayed = TraceReplay.replayTraceFile(stateSpace, replayTrace.getAbsoluteLocation());
		List<ErrorItem> errors = replayed.getErrors();
		if (errors.isEmpty() && replayed.getReplayStatus() != TraceReplayStatus.PERFECT) {
			// FIXME Should this case be reported as an error on the Prolog side?
			final ErrorItem error = new ErrorItem("Trace could not be replayed completely", ErrorItem.Type.ERROR, Collections.emptyList());
			errors = new ArrayList<>(errors);
			errors.add(error);
		}
		replayed = replayed.withErrors(errors);
		Trace trace = replayed.getTrace(stateSpace);
		replayTrace.setResult(new ReplayTrace.Result(replayed, trace));
	}

	public static void checkNoninteractive(ReplayTrace replayTrace, StateSpace stateSpace) {
		try {
			replayTrace.reset();
			replayTrace.setResult(new CheckingResult(CheckingStatus.IN_PROGRESS));
			checkNoninteractiveInternal(replayTrace, stateSpace);
		} catch (CommandInterruptedException exc) {
			LOGGER.info("Trace check interrupted by user", exc);
			replayTrace.setResult(new CheckingResult(CheckingStatus.INTERRUPTED));
		} catch (RuntimeException exc) {
			replayTrace.setResult(new CheckingResult(CheckingStatus.INVALID_TASK, "common.result.message", exc.toString()));
			throw exc;
		}
	}

	private void showTraceReplayCompleteFailed(final ReplayTrace replayTrace) {
		Platform.runLater(() -> {
			ReplayedTraceStatusAlert alert = replayedAlertProvider.get();
			alert.initReplayTrace(replayTrace);
			alert.showAndWait().ifPresent(buttonType -> {
				if (buttonType.equals(alert.getAcceptButtonType())) {
					currentTrace.set(replayTrace.getTrace());
				}
			});
		});
	}

	public void setCurrentTraceAfterReplay(final ReplayTrace replayTrace) {
		// set the current trace if no error has occurred. Otherwise leave the decision to the user
		var traceResult = (ReplayTrace.Result)replayTrace.getResult();
		if (!traceResult.getReplayed().getErrors().isEmpty()) {
			showTraceReplayCompleteFailed(replayTrace);
		} else {
			currentTrace.set(traceResult.getTrace());
		}
	}
}
