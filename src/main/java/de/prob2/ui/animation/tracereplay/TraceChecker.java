package de.prob2.ui.animation.tracereplay;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import de.prob.statespace.Trace;
import de.prob2.ui.internal.FXMLInjected;

import javafx.application.Platform;
import javafx.scene.control.ButtonType;

@FXMLInjected
@Singleton
public final class TraceChecker {
	private final Provider<ReplayedTraceStatusAlert> replayedAlertProvider;

	@Inject
	private TraceChecker(Provider<ReplayedTraceStatusAlert> replayedAlertProvider) {
		this.replayedAlertProvider = replayedAlertProvider;
	}

	private CompletableFuture<Optional<Trace>> showTraceReplayCompleteFailed(final ReplayTrace replayTrace) {
		CompletableFuture<Optional<Trace>> future = new CompletableFuture<>();
		Platform.runLater(() -> {
			ReplayedTraceStatusAlert alert = replayedAlertProvider.get();
			alert.initReplayTrace(replayTrace);
			Optional<ButtonType> result = alert.showAndWait();
			if (result.isPresent() && result.get().equals(alert.getAcceptButtonType())) {
				future.complete(Optional.of(replayTrace.getTrace()));
			} else {
				future.complete(Optional.empty());
			}
		});
		return future;
	}

	/**
	 * Ask the user whether a replayed trace should be accepted or discarded.
	 * The user is only prompted if the replay was not fully successful (i. e. there were errors).
	 * A perfectly replayed trace is always accepted without asking the user.
	 * 
	 * @param replayTrace the trace task that was replayed
	 * @return the trace to be used as the new current trace, or {@link Optional#empty()} if the current trace should be left unchanged (i. e. the user discarded the replayed trace)
	 */
	public CompletableFuture<Optional<Trace>> askKeepReplayedTrace(final ReplayTrace replayTrace) {
		var traceResult = (ReplayTrace.Result)replayTrace.getResult();
		if (!traceResult.getReplayed().getErrors().isEmpty()) {
			return showTraceReplayCompleteFailed(replayTrace);
		} else {
			return CompletableFuture.completedFuture(Optional.of(traceResult.getTrace()));
		}
	}
}
