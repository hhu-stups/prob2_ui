package de.prob2.ui.animation.tracereplay;

import com.google.inject.Injector;
import de.prob.check.tracereplay.PersistentTransition;
import de.prob.check.tracereplay.ReplayedTrace;
import de.prob.check.tracereplay.TraceReplayStatus;
import de.prob.check.tracereplay.TransitionReplayPrecision;
import de.prob.check.tracereplay.json.storage.TraceJsonFile;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import de.prob2.ui.error.ErrorTableView;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.operations.OperationItem;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.Region;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ReplayedTraceStatusAlert extends Alert {

	@FXML
	private ReplayedTraceTable traceTable;

	@FXML
	private ErrorTableView errorTable;

	private final StageManager stageManager;
	private final TraceFileHandler traceFileHandler;
	private final I18n i18n;
	private final ReplayTrace replayTrace;

	public ReplayedTraceStatusAlert(Injector injector, ReplayTrace replayTrace) {
		super(AlertType.NONE);
		this.stageManager = injector.getInstance(StageManager.class);
		this.traceFileHandler = injector.getInstance(TraceFileHandler.class);
		this.i18n = injector.getInstance(I18n.class);
		this.replayTrace = Objects.requireNonNull(replayTrace, "replayTrace");

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

		if (replayedTrace != null) {
			this.setHeaderText(i18n.translate("animation.tracereplay.replayedStatus.headerWithReplayStatus", replayedTrace.getReplayStatus()));
			this.errorTable.getErrorItems().setAll(replayedTrace.getErrors());
		} else {
			this.setHeaderText(i18n.translate("animation.tracereplay.replayedStatus.headerWithoutReplayStatus"));
			this.errorTable.getErrorItems().clear();
		}

		ObservableList<ReplayedTraceRow> items;
		try {
			items = buildRows(replayTrace);
		} catch (IOException e) {
			Platform.runLater(() -> {
				this.close();
				traceFileHandler.showLoadError(replayTrace.getAbsoluteLocation(), e);
			});
			return;
		}

		this.traceTable.setItems(items);
		if (replayedTrace == null || traceFromReplayed == null) {
			this.traceTable.disableReplayedTransitionColumns();
		}
	}

	private static ObservableList<ReplayedTraceRow> buildRows(ReplayTrace replayTrace) throws IOException {
		ReplayedTrace replayedTrace = replayTrace.getReplayedTrace();
		Trace traceFromReplayed = replayTrace.getAnimatedReplayedTrace();
		TraceJsonFile fileTrace = replayTrace.getLoadedTrace();
		if (fileTrace == null) {
			fileTrace = replayTrace.load();
		}

		ObservableList<ReplayedTraceRow> items = FXCollections.observableArrayList();

		int transitionCount = fileTrace.getTransitionList().size();
		if (traceFromReplayed != null) {
			transitionCount = Math.min(transitionCount, traceFromReplayed.getTransitionList().size());
		}
		if (traceFromReplayed != null) {
			transitionCount = Math.min(transitionCount, traceFromReplayed.getTransitionList().size());
		}
		if (replayedTrace != null) {
			transitionCount = Math.min(transitionCount, Math.min(replayedTrace.getTransitionReplayPrecisions().size(), replayedTrace.getTransitionErrorMessages().size()));
		}

		for (int i = 0; i < transitionCount; i++) {
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

		return items;
	}

	private static boolean isError(ReplayTrace replayTrace) {
		return replayTrace.getReplayedTrace() != null && (!replayTrace.getReplayedTrace().getErrors().isEmpty() || replayTrace.getReplayedTrace().getReplayStatus() != TraceReplayStatus.PERFECT);
	}
}
