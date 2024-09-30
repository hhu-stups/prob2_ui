package de.prob2.ui.animation.tracereplay;

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
