package de.prob2.ui.beditor;


import com.google.inject.Inject;

import de.prob2.ui.internal.StageManager;
import javafx.fxml.FXML;
import javafx.stage.Stage;

public class BEditorStage extends Stage {
	
	@FXML
	private Editor beditor;
	
	@Inject
	public BEditorStage(final StageManager stageManager) {
		stageManager.loadFXML(this, "beditor.fxml");
	}
	
	public void setTextEditor(String text) {
		beditor.clear();
		beditor.appendText(text);
	}

}
