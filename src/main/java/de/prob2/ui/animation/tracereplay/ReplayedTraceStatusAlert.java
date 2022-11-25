package de.prob2.ui.animation.tracereplay;

import com.google.inject.Injector;
import de.prob.check.tracereplay.PersistentTransition;
import de.prob.check.tracereplay.ReplayedTrace;
import de.prob.check.tracereplay.json.storage.TraceJsonFile;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.Region;

import java.io.IOException;
import java.io.UncheckedIOException;

public class ReplayedTraceStatusAlert extends Alert {

	@FXML
	private ReplayedTraceTable traceTable;

	private final StageManager stageManager;
	private final I18n i18n;
	private final ReplayTrace replayTrace;

	public ReplayedTraceStatusAlert(Injector injector, ReplayTrace replayTrace) {
		super(AlertType.NONE);
		this.stageManager = injector.getInstance(StageManager.class);
		this.i18n = injector.getInstance(I18n.class);
		this.replayTrace = replayTrace;

		stageManager.loadFXML(this, "trace_replay_status_alert.fxml");
	}

	@FXML
	private void initialize() {
		stageManager.register(this);

		this.setAlertType(isError(replayTrace) ? AlertType.ERROR : AlertType.INFORMATION);
		this.getButtonTypes().setAll(ButtonType.OK);

		this.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);

		ReplayedTrace replayedTrace = replayTrace.getReplayedTrace();
		Trace traceFromReplayed = replayTrace.getAnimatedReplayedTrace();
		TraceJsonFile fileTrace = replayTrace.getLoadedTrace();
		if (fileTrace == null) {
			try {
				fileTrace = replayTrace.load();
			} catch (IOException e) {
				throw new UncheckedIOException("cannot load trace", e);
			}
		}

		this.setHeaderText("%Trace Replay Status: " + replayedTrace.getReplayStatus() /*i18n.translate("animation.tracereplay.alerts.traceReplayError.header")*/);

		ObservableList<ReplayedTraceRow> items = FXCollections.observableArrayList();
		for (int i = 0; i < fileTrace.getTransitionList().size(); i++) {
			PersistentTransition fileTransitionObj = fileTrace.getTransitionList().get(i);
			Transition replayedTransitionObj = traceFromReplayed.getTransitionList().get(i);

			int step = i + 1;
			String fileTransition = fileTransitionObj.getOperationName();
			String replayedTransition = replayedTransitionObj.getName();

			items.add(new ReplayedTraceRow(step, fileTransition, replayedTransition));
		}

		traceTable.setItems(items);
	}

	private static boolean isError(ReplayTrace replayTrace) {
		return !replayTrace.getReplayedTrace().getErrors().isEmpty();
	}
}
