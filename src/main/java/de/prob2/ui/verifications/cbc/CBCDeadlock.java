package de.prob2.ui.verifications.cbc;


import javax.inject.Inject;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class CBCDeadlock extends Stage {
	
	@FXML
	private ChoiceBox<String> cbOperations;
	
	private final CurrentTrace currentTrace;

	@Inject
	private CBCDeadlock(final StageManager stageManager, final CurrentTrace currentTrace) {
		this.currentTrace = currentTrace;
		stageManager.loadFXML(this, "cbc_deadlock.fxml");
		this.initModality(Modality.APPLICATION_MODAL);
		
	}
		
}
