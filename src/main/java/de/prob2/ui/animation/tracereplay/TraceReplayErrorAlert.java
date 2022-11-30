package de.prob2.ui.animation.tracereplay;

import com.google.inject.Injector;

import de.prob.check.tracereplay.json.storage.TraceJsonFile;
import de.prob.statespace.Trace;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.tracediff.TraceDiffStage;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;

public final class TraceReplayErrorAlert extends Alert {
	public enum Trigger {
		TRIGGER_SAVE_TRACE,
		@Deprecated
		TRIGGER_TRACE_CHECKER,
		@Deprecated
		TRIGGER_TRACE_REPLAY_VIEW
	}

	@FXML
	private Text error;
	@FXML
	private TextArea taError;

	private final Injector injector;
	private final StageManager stageManager;
	private final I18n i18n;
	private final String text;
	private final Trigger trigger;
	private int attemptedReplayTraceSize = -1;
	private int storedTraceSize = -1;
	private int lineNumber = -1;
	private ButtonType showTraceDiff;
	private ButtonType showNewStatusAlert;
	private ReplayTrace replayTrace = null;
	private Trace attemptedReplayTrace = null;
	private TraceJsonFile storedTrace = null;
	private Trace history = null;

	public TraceReplayErrorAlert(final Injector injector, final String contentBundleKey, Trigger trigger, final Object... contentParams) {
		super(AlertType.ERROR);
		this.injector = injector;
		this.stageManager = injector.getInstance(StageManager.class);
		this.i18n = injector.getInstance(I18n.class);
		this.text = i18n.translate(contentBundleKey, contentParams);
		this.trigger = trigger;

		stageManager.loadFXML(this, "trace_replay_error_alert.fxml");
	}

	void setLineNumber(int lineNumber) {this.lineNumber = lineNumber;}

	public void setReplayTrace(ReplayTrace replayTrace) {
		this.replayTrace = replayTrace;
	}

	public void setAttemptedReplayOrLostTrace(Trace copyFailedTrace) {
		this.attemptedReplayTrace = copyFailedTrace;
		this.attemptedReplayTraceSize = copyFailedTrace.getTransitionList().size();
	}

	void setStoredTrace(TraceJsonFile traceJsonFile) {
		this.storedTrace = traceJsonFile;
		this.storedTraceSize = traceJsonFile.getTransitionList().size();
	}

	void setHistory(Trace history) {
		this.history = history;
	}

	@FXML
	private void initialize() {
		this.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
		this.setHeaderText(i18n.translate("animation.tracereplay.alerts.traceReplayError.header"));
		stageManager.register(this);
		taError.setText(text);
		double padding = this.getDialogPane().getPadding().getRight() + this.getDialogPane().getPadding().getLeft();
		error.wrappingWidthProperty().bind(this.getDialogPane().widthProperty().subtract(padding));
		this.showTraceDiff = new ButtonType(i18n.translate("animation.tracereplay.alerts.traceReplayError.error.traceDiff"));
		this.showNewStatusAlert = new ButtonType(i18n.translate("animation.tracereplay.alerts.traceReplayError.replayStatus"));
		setButtons();
	}

	private void setButtons() {
		switch (trigger) {
			case TRIGGER_TRACE_REPLAY_VIEW:
				this.getButtonTypes().add(ButtonType.CLOSE);
				break;
			case TRIGGER_SAVE_TRACE:
				/*this.showTraceDiff = new ButtonType(i18n.translate("animation.tracereplay.alerts.traceReplayError.error.traceDiff"));
				this.getButtonTypes().addAll(ButtonType.YES, this.showTraceDiff, ButtonType.NO);*/
				// TODO Find a way to distinguish between current trace and history (might be not the same if storing failed)
				this.getButtonTypes().addAll(ButtonType.YES, ButtonType.NO);
				break;
			case TRIGGER_TRACE_CHECKER:
				this.getButtonTypes().addAll(this.showTraceDiff, this.showNewStatusAlert, ButtonType.CLOSE);
				break;
			default:
				// Trigger has not been added yet.
		}
	}

	public void setErrorMessage() {
		switch (trigger) {
			case TRIGGER_SAVE_TRACE:
				this.setHeaderText(i18n.translate("traceSave.buttons.saveTrace.error"));
				error.setText(i18n.translate("traceSave.buttons.saveTrace.error.msg"));
				this.getDialogPane().setExpandableContent(null);
				handleAlert();
				break;
			case TRIGGER_TRACE_CHECKER:
				error.setText(i18n.translate("animation.tracereplay.alerts.traceReplayError.error", attemptedReplayTraceSize, storedTraceSize, lineNumber));
				handleAlert();
				break;
			case TRIGGER_TRACE_REPLAY_VIEW:
				error.setText(text);
				this.getDialogPane().setExpandableContent(null);
				this.show();
				break;
			default:
				// Trigger has not been added yet.
		}
	}

	public Trigger getTrigger() {
		return trigger;
	}

	public void showAlertAgain() {
		handleAlert();
	}

	private void handleAlert() {
		injector.getInstance(TraceDiffStage.class).close();
		CurrentTrace currentTrace = injector.getInstance(CurrentTrace.class);
		ButtonType type = this.showAndWait().orElse(null);
		if (type == ButtonType.NO) {
			currentTrace.set(history == null ? new Trace(currentTrace.getStateSpace()) : history);
		} else if (type == showTraceDiff) {
			TraceDiffStage traceDiffStage = injector.getInstance(TraceDiffStage.class);
			traceDiffStage.setAlert(this);
			if (trigger == Trigger.TRIGGER_SAVE_TRACE) {
				traceDiffStage.setLists(attemptedReplayTrace, history);
			} else {
				traceDiffStage.setLists(attemptedReplayTrace, storedTrace);
			}
			traceDiffStage.show();
		} else if (type == showNewStatusAlert) {
			new ReplayedTraceStatusAlert(injector, replayTrace).show();
		}
	}
}
