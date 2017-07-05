package de.prob2.ui.verifications.cbc;

import javax.inject.Inject;

import de.prob2.ui.internal.StageManager;
import javafx.fxml.FXML;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class CBCChoosingStage extends Stage {
	
	@Inject
	private CBCChoosingStage(final StageManager stageManager) {
		stageManager.loadFXML(this, "cbc_checking_choice.fxml");
		this.initModality(Modality.APPLICATION_MODAL);
	}
	
	@FXML
	public void choose() {
		
	}

}
