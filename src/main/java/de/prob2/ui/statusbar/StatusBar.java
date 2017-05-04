package de.prob2.ui.statusbar;

import java.util.ResourceBundle;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

@Singleton
public class StatusBar extends HBox {
	@FXML private Label invariantOkLabel;
	
	private final ResourceBundle resourceBundle;
	private final CurrentTrace currentTrace;
	
	@Inject
	private StatusBar(final ResourceBundle resourceBundle, final CurrentTrace currentTrace, final StageManager stageManager) {
		super();
		
		this.resourceBundle = resourceBundle;
		this.currentTrace = currentTrace;
		
		stageManager.loadFXML(this, "status_bar.fxml");
	}
	
	@FXML
	private void initialize() {
		this.currentTrace.addListener((observable, from, to) -> this.update());
	}
	
	private void update() {
		invariantOkLabel.getStyleClass().removeAll("false", "true");
		if (!this.currentTrace.exists()) {
			invariantOkLabel.setText(this.resourceBundle.getString("common.noModelLoaded"));
		} else if (this.currentTrace.getCurrentState().isInvariantOk()) {
			invariantOkLabel.setText(this.resourceBundle.getString("statusbar.invariantOk.true"));
			invariantOkLabel.getStyleClass().add("true");
		} else {
			invariantOkLabel.setText(this.resourceBundle.getString("statusbar.invariantOk.false"));
			invariantOkLabel.getStyleClass().add("false");
		}
	}
}
