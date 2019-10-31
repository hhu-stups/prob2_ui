package de.prob2.ui.unsatcore;


import com.google.inject.Inject;

import de.prob2.ui.internal.StageManager;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

public class UnsatCoreStage extends Stage {
	
	@FXML
	private TextArea taUnsatCore;
	
	@Inject
	public UnsatCoreStage(final StageManager stageManager) {
		stageManager.loadFXML(this, "unsatcore_stage.fxml");
	}
	
	public void setUnsatCore(String text) {
		taUnsatCore.setText(text);
	}
	
}
