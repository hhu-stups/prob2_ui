package de.prob2.ui.animation.tracereplay;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.stream.Collectors;

import com.google.inject.Injector;

import de.prob.check.tracereplay.PersistentTransition;
import de.prob.check.tracereplay.ReplayedTrace;
import de.prob.check.tracereplay.TransitionReplayPrecision;
import de.prob.check.tracereplay.json.storage.TraceJsonFile;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import de.prob2.ui.error.ErrorTableView;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.operations.OperationItem;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.Region;

public class ReplayedTraceStatusAlert extends Alert {

	@FXML
	private ReplayedTraceTable traceTable;

	@FXML
	private ErrorTableView errorTable;

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

		this.errorTable.dontSyncWithEditor();
		this.errorTable.visibleProperty().bind(Bindings.createBooleanBinding(() -> !this.errorTable.getErrorItems().isEmpty(), this.errorTable.getErrorItems()));

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

		if (replayedTrace != null) {
			this.setHeaderText(i18n.translate("animation.tracereplay.replayedStatus.headerWithReplayStatus", replayedTrace.getReplayStatus()));
			this.errorTable.getErrorItems().setAll(replayedTrace.getErrors());
		} else {
			this.setHeaderText(i18n.translate("animation.tracereplay.replayedStatus.headerWithoutReplayStatus"));
			this.errorTable.getErrorItems().clear();
		}

		ObservableList<ReplayedTraceRow> items = FXCollections.observableArrayList();
		for (int i = 0; i < fileTrace.getTransitionList().size(); i++) {
			PersistentTransition fileTransitionObj = fileTrace.getTransitionList().get(i);
			Transition replayedTransitionObj = traceFromReplayed != null ? traceFromReplayed.getTransitionList().get(i) : null;
			TransitionReplayPrecision transitionReplayPrecision = replayedTrace != null ? replayedTrace.getTransitionReplayPrecisions().get(i) : null;
			List<String> transitionErrorMessages = replayedTrace != null ? replayedTrace.getTransitionErrorMessages().get(i) : null;

			int step = i + 1;
			String fileTransition;
			{
				fileTransition = Transition.prettifyName(fileTransitionObj.getOperationName());

				// if we want to show all state changes use this code (as it was in the TraceDiff but not in HistoryView)
				/*String args = Stream.concat(
						fileTransitionObj.getParameters().entrySet().stream()
								.map(e -> e.getKey() + "=" + e.getValue()),
						Transition.isArtificialTransitionName(fileTransitionObj.getOperationName()) ?
								fileTransitionObj.getDestinationStateVariables().entrySet().stream()
										.map(e -> e.getKey() + ":=" + e.getValue()) :
								Stream.empty()
				).collect(Collectors.joining(", "));*/
				// this code does not show these state variable changes, which is more in line with OperationItem#prettyPrint
				String args = fileTransitionObj.getParameters().entrySet().stream()
						              .map(e -> e.getKey() + "=" + e.getValue())
						              .collect(Collectors.joining(", "));
				if (!args.isEmpty()) {
					fileTransition += "(" + args + ")";
				}

				String outputArgs = String.join(", ", fileTransitionObj.getOutputParameters().values());
				if (!outputArgs.isEmpty()) {
					fileTransition += " → " + outputArgs;
				}
			}

			String replayedTransition;
			if (replayedTransitionObj != null) {
				OperationItem opItem = OperationItem.forTransitionFast(replayedTransitionObj.getStateSpace(), replayedTransitionObj);
				replayedTransition = opItem.toPrettyString(true);
			} else {
				replayedTransition = "";
			}

			String precision = transitionReplayPrecision != null ? transitionReplayPrecision.toString() : ""; // TODO: pretty name
			String errorMessage = transitionErrorMessages != null ? String.join(" ", transitionErrorMessages) : ""; // TODO: prettify

			items.add(new ReplayedTraceRow(step, fileTransition, replayedTransition, precision, errorMessage));
		}

		this.traceTable.setItems(items);
		if (replayedTrace == null || traceFromReplayed == null) {
			this.traceTable.disableReplayedTransitionColumns();
		}
	}

	private static boolean isError(ReplayTrace replayTrace) {
		return replayTrace.getReplayedTrace() != null && !replayTrace.getReplayedTrace().getErrors().isEmpty();
	}
}
