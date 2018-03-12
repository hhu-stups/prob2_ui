package de.prob2.ui.internal;

import com.google.inject.Inject;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

public class DynamicCommandStatusBar extends HBox {

	@FXML 
	private Label statusLabel;
	
	@Inject
	public DynamicCommandStatusBar(final StageManager stageManager) {
		super();
		System.out.println("test");
		stageManager.loadFXML(this, "dynamic_command_status_bar.fxml");
	}
	
	@FXML
	private void initialize() {}
	
	public void setText(String text) {
		statusLabel.setText(text);
	}
}
