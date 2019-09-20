package de.prob2.ui.error;

import java.util.List;
import java.util.Objects;

import de.prob.animator.domainobjects.ErrorItem;
import de.prob2.ui.internal.StageManager;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public final class WarningAlert extends Alert {
	@FXML private VBox contentVBox;
	@FXML private Label label;
	@FXML private ErrorTableView warningTable;
	
	private final StageManager stageManager;
	private final List<ErrorItem> warnings;
	
	public WarningAlert(final StageManager stageManager, final List<ErrorItem> warnings) {
		super(AlertType.NONE); // Alert type is set in FXML
		
		Objects.requireNonNull(warnings);
		
		this.stageManager = stageManager;
		this.warnings = warnings;
		
		stageManager.loadFXML(this, "warning_alert.fxml");
	}
	
	@FXML
	private void initialize() {
		stageManager.register(this);
		
		this.warningTable.getErrorItems().setAll(this.warnings);
	}
}
