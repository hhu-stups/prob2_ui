package de.prob2.ui.dynamic;

import com.google.inject.Inject;

import de.prob2.ui.internal.StageManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

public class DynamicCommandStatusBar extends HBox {

	@FXML 
	private Label statusLabel;
	
	@Inject
	public DynamicCommandStatusBar(final StageManager stageManager) {
		super();
		stageManager.loadFXML(this, "dynamic_command_status_bar.fxml");
	}
	
	public void setText(String text) {
		statusLabel.setText(text);
	}
}
