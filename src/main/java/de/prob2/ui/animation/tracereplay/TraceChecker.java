package de.prob2.ui.animation.tracereplay;

import java.util.concurrent.CompletableFuture;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.prob2fx.CurrentTrace;

import javafx.application.Platform;

@FXMLInjected
@Singleton
public final class TraceChecker {
	private final CurrentTrace currentTrace;
	private final Provider<ReplayedTraceStatusAlert> replayedAlertProvider;

	@Inject
	private TraceChecker(CurrentTrace currentTrace, Provider<ReplayedTraceStatusAlert> replayedAlertProvider) {
		this.currentTrace = currentTrace;
		this.replayedAlertProvider = replayedAlertProvider;
	}

	private CompletableFuture<?> showTraceReplayCompleteFailed(final ReplayTrace replayTrace) {
		CompletableFuture<?> future = new CompletableFuture<>();
		Platform.runLater(() -> {
			ReplayedTraceStatusAlert alert = replayedAlertProvider.get();
			alert.initReplayTrace(replayTrace);
			alert.showAndWait().ifPresent(buttonType -> {
				if (buttonType.equals(alert.getAcceptButtonType())) {
					currentTrace.set(replayTrace.getTrace());
				}
			});
			future.complete(null);
		});
		return future;
	}

	public CompletableFuture<?> setCurrentTraceAfterReplay(final ReplayTrace replayTrace) {
		// set the current trace if no error has occurred. Otherwise leave the decision to the user
		var traceResult = (ReplayTrace.Result)replayTrace.getResult();
		if (!traceResult.getReplayed().getErrors().isEmpty()) {
			return showTraceReplayCompleteFailed(replayTrace);
		} else {
			currentTrace.set(traceResult.getTrace());
			return CompletableFuture.completedFuture(null);
		}
	}
}
