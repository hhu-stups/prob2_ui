package de.prob2.ui.error;

import com.google.inject.Inject;

import de.prob.animator.domainobjects.ErrorItem;
import de.prob2.ui.internal.StageManager;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
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
	private ObservableList<ErrorItem> warnings;
	
	@Inject
	private WarningAlert(final StageManager stageManager) {
		super(Alert.AlertType.NONE); // Alert type is set in FXML
		
		this.stageManager = stageManager;
		this.warnings = FXCollections.observableArrayList();
		
		stageManager.loadFXML(this, "warning_alert.fxml");
	}
	
	@FXML
	private void initialize() {
		stageManager.register(this);
		
		Bindings.bindContent(this.warningTable.getErrorItems(), this.getWarnings());
	}
	
	public ObservableList<ErrorItem> getWarnings() {
		return warnings;
	}
}
