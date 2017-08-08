package de.prob2.ui.verifications.cbc;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;

import javax.inject.Inject;

public class CBCSequence extends Stage {
	
	private final CurrentTrace currentTrace;

	@FXML
	private TextField tfSequence;
	
	@Inject
	private CBCSequence(final StageManager stageManager, final CurrentTrace currentTrace) {
		this.currentTrace = currentTrace;
		stageManager.loadFXML(this, "cbc_sequence.fxml");
		this.initModality(Modality.APPLICATION_MODAL);
	}
	
	
}
