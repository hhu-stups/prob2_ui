package de.prob2.ui.animation.tracereplay;


import com.google.inject.Injector;
import de.prob.check.tracereplay.PersistentTrace;
import de.prob.statespace.Trace;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.tracediff.TraceDiffStage;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;

import java.util.Optional;
import java.util.ResourceBundle;


public final class TraceReplayErrorAlert extends Alert {
	public enum Trigger {
		TRIGGER_VISB, TRIGGER_HISTORY_VIEW, TRIGGER_TRACE_CHECKER, TRIGGER_TRACE_REPLAY_VIEW
	}

	@FXML
	private Text error;
	@FXML 
	private TextArea taError;

	private final Injector injector;
	private final StageManager stageManager;
	private final ResourceBundle bundle;
	private final String text;
	private final Trigger trigger;
	private int traceSize = -1;
	private int persistentTraceSize = -1;
	private int lineNumber = -1;
	private ButtonType showTraceDiff;
	private Trace copyTrace = null;
	private PersistentTrace persistentTrace = null;
	
	public TraceReplayErrorAlert(final Injector injector, final String contentBundleKey, Trigger trigger, final Object... contentParams) {
		super(AlertType.ERROR);
		this.injector = injector;
		this.stageManager = injector.getInstance(StageManager.class);
		this.bundle = injector.getInstance(ResourceBundle.class);
		this.text = String.format(bundle.getString(contentBundleKey), contentParams);
		this.trigger = trigger;
		
		stageManager.loadFXML(this, "trace_replay_error_alert.fxml");
	}

	void setTraceSize(int traceSize) {this.traceSize = traceSize;}

	void setPersistentTraceSize(int persistentTraceSize) {this.persistentTraceSize = persistentTraceSize;}

	void setLineNumber(int lineNumber) {this.lineNumber = lineNumber;}

	public void setCopyTrace(Trace copyTrace) {this.copyTrace = copyTrace;}

	void setPersistentTrace(PersistentTrace persistentTrace) {this.persistentTrace = persistentTrace;}
	
	@FXML
	private void initialize() {
		this.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
		this.setHeaderText(bundle.getString("animation.tracereplay.alerts.traceReplayError.header"));
		stageManager.register(this);
		taError.setText(text);
		double padding = this.getDialogPane().getPadding().getRight() + this.getDialogPane().getPadding().getLeft();
		error.wrappingWidthProperty().bind(this.getDialogPane().widthProperty().subtract(padding));
		setButtons();
	}

	private void setButtons() {
		switch (trigger) {
			case TRIGGER_TRACE_REPLAY_VIEW:
				this.getButtonTypes().add(ButtonType.CLOSE);
				break;
			case TRIGGER_VISB:
			case TRIGGER_HISTORY_VIEW:
			case TRIGGER_TRACE_CHECKER:
				this.showTraceDiff = new ButtonType(injector.getInstance(ResourceBundle.class).getString("animation.tracereplay.alerts.traceReplayError.error.traceDiff"));
				this.getButtonTypes().addAll(ButtonType.YES, this.showTraceDiff, ButtonType.NO);
				break;
			default:
				// Trigger has not been added yet.
		}
	}

	public void setErrorMessage() {
		setErrorMessage(false);
	}

	void setErrorMessage(boolean tracesAreEqual) {
		switch (trigger) {
			case TRIGGER_VISB:
			case TRIGGER_HISTORY_VIEW:
				this.setHeaderText(bundle.getString("traceSave.buttons.saveTrace.error"));
				error.setText(bundle.getString("traceSave.buttons.saveTrace.error.msg"));
				this.getDialogPane().setExpandableContent(null);
				handleAlert(copyTrace, null);
				break;
			case TRIGGER_TRACE_CHECKER:
				error.setText(String.format(bundle.getString("animation.tracereplay.alerts.traceReplayError.error"), traceSize, persistentTraceSize, lineNumber));
				handleAlert(copyTrace, persistentTrace);
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
		handleAlert(copyTrace, persistentTrace);
	}

	private void handleAlert(Trace copyTrace, PersistentTrace persistentTrace) {
		injector.getInstance(TraceDiffStage.class).close();
		CurrentTrace currentTrace = injector.getInstance(CurrentTrace.class);
		Optional<ButtonType> type = this.showAndWait();
		if (type.get() == ButtonType.YES) {
			currentTrace.set(copyTrace);
		} else if (type.get() == showTraceDiff) {
			TraceDiffStage traceDiffStage = injector.getInstance(TraceDiffStage.class);
			traceDiffStage.setAlert(this);
			traceDiffStage.setLists(copyTrace, persistentTrace, currentTrace.get());
			traceDiffStage.show();
		}
	}
}
