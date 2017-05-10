package de.prob2.ui.statusbar;

import java.util.ArrayList;
import java.util.List;
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
	@FXML private Label errorsLabel;
	
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
		errorsLabel.getStyleClass().removeAll("noErrors", "someErrors");
		if (this.currentTrace.exists()) {
			final List<String> errorMessages = new ArrayList<>();
			if (!this.currentTrace.getCurrentState().isInvariantOk()) {
				errorMessages.add(resourceBundle.getString("statusbar.errors.invariantNotOK"));
			}
			if (!this.currentTrace.getCurrentState().getStateErrors().isEmpty()) {
				errorMessages.add(resourceBundle.getString("statusbar.errors.stateErrors"));
			}
			
			if (errorMessages.isEmpty()) {
				errorsLabel.getStyleClass().add("noErrors");
				errorsLabel.setText(resourceBundle.getString("statusbar.noErrors"));
			} else {
				errorsLabel.getStyleClass().add("someErrors");
				errorsLabel.setText(String.format(resourceBundle.getString("statusbar.someErrors"), String.join(", ", errorMessages)));
			}
		} else {
			errorsLabel.setText(this.resourceBundle.getString("common.noModelLoaded"));
		}
	}
}
