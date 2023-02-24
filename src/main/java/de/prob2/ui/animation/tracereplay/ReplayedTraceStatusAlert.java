package de.prob2.ui.animation.tracereplay;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import de.prob2.ui.internal.StopActions;
import de.prob2.ui.internal.executor.CliTaskExecutor;
import de.prob2.ui.internal.executor.CompletableExecutorService;
import de.prob2.ui.internal.executor.CompletableThreadPoolExecutor;
import de.prob2.ui.operations.OperationItem;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.Region;

import static de.prob2.ui.internal.TranslatableAdapter.enumNameAdapter;

public class ReplayedTraceStatusAlert extends Alert {

	private final StageManager stageManager;
	private final TraceFileHandler traceFileHandler;
	private final CliTaskExecutor cliExecutor;
	private final I18n i18n;
	private final CompletableExecutorService executor;
	private final ReplayTrace replayTrace;
	@FXML
	private ReplayedTraceTable traceTable;
	@FXML
	private ErrorTableView errorTable;

	public ReplayedTraceStatusAlert(Injector injector, ReplayTrace replayTrace) {
		super(AlertType.NONE);
		this.stageManager = injector.getInstance(StageManager.class);
		this.traceFileHandler = injector.getInstance(TraceFileHandler.class);
		this.i18n = injector.getInstance(I18n.class);
		this.cliExecutor = injector.getInstance(CliTaskExecutor.class);
		this.executor = CompletableThreadPoolExecutor.newSingleThreadedExecutor(r -> new Thread(r, "Trace replay status thread"));
		injector.getInstance(StopActions.class).add(this.executor::shutdownNow);
		this.replayTrace = Objects.requireNonNull(replayTrace, "replayTrace");

		stageManager.loadFXML(this, "trace_replay_status_alert.fxml");
	}

	private static boolean isError(ReplayTrace replayTrace) {
		return replayTrace.getReplayedTrace() != null && (!replayTrace.getReplayedTrace().getErrors().isEmpty() || replayTrace.getReplayedTrace().getReplayStatus() != TraceReplayStatus.PERFECT);
	}

	@FXML
	private void initialize() {
		stageManager.register(this);

		this.setAlertType(isError(replayTrace) ? AlertType.ERROR : AlertType.INFORMATION);
		this.getButtonTypes().setAll(ButtonType.OK);

		this.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);

		this.errorTable.dontSyncWithEditor();
		this.errorTable.visibleProperty().bind(Bindings.createBooleanBinding(() -> !this.errorTable.getErrorItems().isEmpty(), this.errorTable.getErrorItems()));
		this.errorTable.managedProperty().bind(this.errorTable.visibleProperty());

		ReplayedTrace replayedTrace = replayTrace.getReplayedTrace();
		Trace traceFromReplayed = replayTrace.getAnimatedReplayedTrace();

		if (replayedTrace != null) {
			this.setHeaderText(i18n.translate("animation.tracereplay.replayedStatus.headerWithReplayStatus", replayedTrace.getReplayStatus()));
			this.errorTable.getErrorItems().setAll(replayedTrace.getErrors());
		} else {
			this.setHeaderText(i18n.translate("animation.tracereplay.replayedStatus.headerWithoutReplayStatus"));
			this.errorTable.getErrorItems().clear();
		}

		if (replayedTrace == null || traceFromReplayed == null) {
			this.traceTable.disableReplayedTransitionColumns();
		}

		executor.submit(this::buildRowsAsync).whenComplete((items, exc) -> {
			if (exc != null) {
				Platform.runLater(() -> {
					this.close();
					traceFileHandler.showLoadError(replayTrace, exc);
				});
			} else if (items != null) {
				Platform.runLater(() -> this.traceTable.setItems(items));
			}
		});
	}

	private ObservableList<ReplayedTraceRow> buildRowsAsync() throws Exception {
		ReplayedTrace replayedTrace = replayTrace.getReplayedTrace();
		Trace traceFromReplayed = replayTrace.getAnimatedReplayedTrace();

		CompletableFuture<Map<Transition, OperationItem>> future;
		if (traceFromReplayed != null) {
			// start cli instantly on another thread, while doing IO on this thread
			future = cliExecutor.submit(() -> OperationItem.forTransitions(
					traceFromReplayed.getStateSpace(),
					traceFromReplayed.getTransitionList()
			));
		} else {
			future = null;
		}

		// always load newest trace file from disk
		TraceJsonFile fileTrace = Objects.requireNonNull(replayTrace.load(), "traceJsonFile");

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

				String args = Stream.concat(
						fileTransitionObj.getParameters().entrySet().stream()
								.map(e -> e.getKey() + "=" + e.getValue()),
						Transition.isArtificialTransitionName(fileTransitionObj.getOperationName()) ?
								fileTransitionObj.getDestinationStateVariables().entrySet().stream()
										.map(e -> e.getKey() + ":=" + e.getValue()) :
								Stream.empty()
				).collect(Collectors.joining(", "));

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
				OperationItem opItem = future.get().get(replayedTransitionObj);
				replayedTransition = opItem.toPrettyString(true);
			} else {
				replayedTransition = "";
			}

			String precision = transitionReplayPrecision != null ? i18n.translate(enumNameAdapter("animation.tracereplay.replayedStatus.transitionReplayPrecision"), transitionReplayPrecision) : "";
			String errorMessage = transitionErrorMessages != null ? String.join("; ", transitionErrorMessages) : ""; // TODO: prettify

			Collection<String> styleClasses;
			if (transitionReplayPrecision != null && transitionReplayPrecision != TransitionReplayPrecision.PRECISE) {
				styleClasses = Collections.singletonList("FAULTY");
			} else {
				styleClasses = Collections.emptyList();
			}

			items.add(new ReplayedTraceRow(step, fileTransition, replayedTransition, precision, errorMessage, null, styleClasses));
		}

		return items;
	}
}
