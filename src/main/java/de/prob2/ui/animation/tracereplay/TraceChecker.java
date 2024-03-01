package de.prob2.ui.animation.tracereplay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.animator.CommandInterruptedException;
import de.prob.animator.domainobjects.ErrorItem;
import de.prob.check.tracereplay.ReplayedTrace;
import de.prob.check.tracereplay.TraceReplay;
import de.prob.check.tracereplay.TraceReplayStatus;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.verifications.Checked;

import javafx.application.Platform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@FXMLInjected
@Singleton
public class TraceChecker {

	private static final Logger LOGGER = LoggerFactory.getLogger(TraceChecker.class);

	private final CurrentTrace currentTrace;
	private final Injector injector;
	private final StageManager stageManager;

	@Inject
	private TraceChecker(final CurrentTrace currentTrace, final Injector injector, final StageManager stageManager) {
		this.currentTrace = currentTrace;
		this.injector = injector;
		this.stageManager = stageManager;
	}

	public void check(ReplayTrace replayTrace) {
		StateSpace stateSpace = currentTrace.getStateSpace();
		try {
			checkNoninteractive(replayTrace, stateSpace);
			setCurrentTraceAfterReplay(replayTrace);
		} catch (RuntimeException exc) {
			Platform.runLater(() -> injector.getInstance(TraceFileHandler.class).showLoadError(replayTrace, exc));
		}
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
		replayTrace.setReplayedTrace(replayed);
		Trace trace = replayed.getTrace(stateSpace);
		replayTrace.setAnimatedReplayedTrace(trace);
		replayTrace.setChecked(errors.isEmpty() ? Checked.SUCCESS : Checked.FAIL);
	}

	public static void checkNoninteractive(ReplayTrace replayTrace, StateSpace stateSpace) {
		try {
			replayTrace.reset();
			// ReplayTraceFileCommand doesn't support progress updates yet,
			// so set an indeterminate status for now.
			// We cannot use -1, because it is already used to say that no replay is currently running
			// (this is special-cased in TraceReplayView).
			Platform.runLater(() -> replayTrace.setProgress(-2));
			checkNoninteractiveInternal(replayTrace, stateSpace);
		} catch (CommandInterruptedException exc) {
			LOGGER.info("Trace check interrupted by user", exc);
			replayTrace.setChecked(Checked.NOT_CHECKED);
		} catch (RuntimeException exc) {
			replayTrace.setChecked(Checked.INVALID_TASK);
			throw exc;
		} finally {
			Platform.runLater(() -> replayTrace.setProgress(-1));
		}
	}

	private void showTraceReplayCompleteFailed(final ReplayTrace replayTrace) {
		Platform.runLater(() -> {
			ReplayedTraceStatusAlert alert = new ReplayedTraceStatusAlert(injector, replayTrace);
			alert.handleAcceptDiscard();
		});
	}

	public void setCurrentTraceAfterReplay(final ReplayTrace replayTrace) {
		// set the current trace if no error has occurred. Otherwise leave the decision to the user
		if (!replayTrace.getReplayedTrace().getErrors().isEmpty()) {
			showTraceReplayCompleteFailed(replayTrace);
		} else {
			currentTrace.set(replayTrace.getAnimatedReplayedTrace());
		}
	}
}
