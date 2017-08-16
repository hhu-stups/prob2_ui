package de.prob2.ui.verifications.cbc;

import de.prob2.ui.internal.StageManager;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;

import javax.inject.Inject;

public class CBCSequence extends Stage {

	@FXML
	private TextField tfSequence;
	
	private final CBCFormulaHandler cbcHandler;
	
	@Inject
	private CBCSequence(final StageManager stageManager, final CBCFormulaHandler cbcHandler) {
		this.cbcHandler = cbcHandler;
		stageManager.loadFXML(this, "cbc_sequence.fxml");
		this.initModality(Modality.APPLICATION_MODAL);
	}
	
	@FXML
	public void addFormula() {
		addFormula(false);
	}
	
	private void addFormula(boolean checking) {
		cbcHandler.addFormula(tfSequence.getText(), tfSequence.getText(), CBCFormulaItem.CBCType.SEQUENCE,
								checking);
	}

	@FXML
	public void checkFormula() {
		addFormula(true);
		cbcHandler.checkSequence(tfSequence.getText());
	}

	@FXML
	public void cancel() {
		this.close();
	}
	
	
}
