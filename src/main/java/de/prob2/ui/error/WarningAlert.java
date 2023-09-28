package de.prob2.ui.error;

import com.google.inject.Inject;

import de.prob.animator.domainobjects.ErrorItem;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public final class WarningAlert extends Alert {
	@FXML private VBox contentVBox;
	@FXML private Label label;
	@FXML private ErrorTableView warningTable;
	
	private final StageManager stageManager;
	private final ObservableList<ErrorItem> warnings;
	private final I18n i18n;
	private String messageKeyForBundle;
	
	@Inject
	private WarningAlert(final StageManager stageManager, I18n i18n) {
		super(Alert.AlertType.NONE); // Alerttype will be set dynamically if warnings change
		
		this.stageManager = stageManager;
		this.warnings = FXCollections.observableArrayList();
		this.i18n = i18n;
		
		stageManager.loadFXML(this, "warning_alert.fxml");
	}
	
	@FXML
	private void initialize() {
		stageManager.register(this);

		warnings.addListener((ListChangeListener<? super ErrorItem>) observable -> {
			if (!observable.getList().isEmpty()) {
				setLevelOfAlert();
			}
		});
		Bindings.bindContent(this.warningTable.getErrorItems(), this.getWarnings());
	}
	
	public ObservableList<ErrorItem> getWarnings() {
		return warnings;
	}

	private void setLevelOfAlert() {
		this.setAlertType(getHighestAlertType());
		this.label.setText(i18n.translate(messageKeyForBundle));
	}

	private Alert.AlertType getHighestAlertType() {
		Alert.AlertType highestAlertType = AlertType.NONE;
		for (ErrorItem errorItem : this.getWarnings()) {
			Alert.AlertType errorItemAlertType;
			switch (errorItem.getType()) {
				case INTERNAL_ERROR:
				case ERROR:
					// No higher information level, return immediately
					messageKeyForBundle = "error.warningAlert.ERROR.content";
					return AlertType.ERROR;
				case WARNING:
					errorItemAlertType = AlertType.WARNING;
					break;
				case MESSAGE:
				default:
					errorItemAlertType = AlertType.INFORMATION;
					break;
			}
			if (highestAlertType.compareTo(errorItemAlertType) < 0) {
				highestAlertType = errorItemAlertType;
				messageKeyForBundle = "error.warningAlert." + highestAlertType + ".content";
			}
		}
		return highestAlertType;
	}
}
