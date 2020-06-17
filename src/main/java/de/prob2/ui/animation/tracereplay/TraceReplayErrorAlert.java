package de.prob2.ui.animation.tracereplay;


import java.util.ResourceBundle;

import com.google.inject.Injector;

import de.prob2.ui.internal.StageManager;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;


public final class TraceReplayErrorAlert extends Alert {

	@FXML
	private Text error;
	@FXML 
	private TextArea taError;

	private final StageManager stageManager;
	private final ResourceBundle bundle;
	private final String text;
	
	public TraceReplayErrorAlert(final Injector injector, final String contentBundleKey, final Object... contentParams) {
		super(AlertType.ERROR);
		this.stageManager = injector.getInstance(StageManager.class);
		this.bundle = injector.getInstance(ResourceBundle.class);
		this.text = String.format(bundle.getString(contentBundleKey), contentParams);
		
		stageManager.loadFXML(this, "trace_replay_error_alert.fxml");
	}
	
	@FXML
	private void initialize() {
		this.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
		this.setHeaderText(bundle.getString("animation.tracereplay.alerts.traceReplayError.header"));
		this.setAlertType(AlertType.ERROR);
		stageManager.register(this);
		taError.setText(text);
		error.wrappingWidthProperty().bind(this.getDialogPane().widthProperty());
	}

	void setErrorMessage(boolean triggeredByErrorItem, boolean tracesAreEqual, int traceSize, int persistentTraceSize) {
		if (triggeredByErrorItem) {
			error.setText(text);
			this.getDialogPane().setExpandableContent(null);
			this.getButtonTypes().add(ButtonType.CLOSE);
		} else {
			if (tracesAreEqual) {
				error.setText(bundle.getString("animation.tracereplay.alerts.traceReplayError.error.tracesAreEqual"));
			} else {
				error.setText(String.format(bundle.getString("animation.tracereplay.alerts.traceReplayError.error.tracesAreNotEqual"), traceSize, persistentTraceSize));
			}
		}
	}
}
