package de.prob2.ui.animation.tracereplay;

import com.google.inject.Injector;

import de.prob.statespace.Trace;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;

public final class TraceSaveErrorAlert extends Alert {

	@FXML
	private Text error;
	@FXML
	private TextArea taError;

	private final Injector injector;
	private final StageManager stageManager;
	private final I18n i18n;
	private final String text;

	public TraceSaveErrorAlert(final Injector injector, final String contentBundleKey, final Object... contentParams) {
		super(AlertType.ERROR);
		this.injector = injector;
		this.stageManager = injector.getInstance(StageManager.class);
		this.i18n = injector.getInstance(I18n.class);
		this.text = i18n.translate(contentBundleKey, contentParams);

		stageManager.loadFXML(this, "trace_replay_error_alert.fxml");
	}

	@FXML
	private void initialize() {
		stageManager.register(this);
		this.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
		this.setHeaderText(i18n.translate("animation.tracereplay.alerts.traceReplayError.header"));
		taError.setText(text);
		double padding = this.getDialogPane().getPadding().getRight() + this.getDialogPane().getPadding().getLeft();
		error.wrappingWidthProperty().bind(this.getDialogPane().widthProperty().subtract(padding));
		setButtons();
	}

	private void setButtons() {
		this.getButtonTypes().addAll(ButtonType.YES, ButtonType.NO);
	}

	public void setErrorMessage() {
		this.setHeaderText(i18n.translate("traceSave.buttons.saveTrace.error"));
		error.setText(i18n.translate("traceSave.buttons.saveTrace.error.msg"));
		this.getDialogPane().setExpandableContent(null);
		handleAlert();
	}

	private void handleAlert() {
		CurrentTrace currentTrace = injector.getInstance(CurrentTrace.class);
		ButtonType type = this.showAndWait().orElse(null);
		if (type == ButtonType.NO) {
			currentTrace.set(new Trace(currentTrace.getStateSpace()));
		}
	}
}
