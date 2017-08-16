package de.prob2.ui.verifications.cbc;

import de.prob2.ui.internal.StageManager;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;

import javax.inject.Inject;

public class CBCDeadlock extends Stage {
	
	private final CBCFormulaHandler cbcHandler;
	
	@FXML
	private TextField tfFormula;

	@Inject
	private CBCDeadlock(final StageManager stageManager, final CBCFormulaHandler cbcHandler) {
		this.cbcHandler = cbcHandler;
		stageManager.loadFXML(this, "cbc_deadlock.fxml");
		this.initModality(Modality.APPLICATION_MODAL);
	}
	
	@FXML
	public void addFormula() {
		addFormula(false);
	}
	
	private void addFormula(boolean checking) {
		cbcHandler.addFormula(tfFormula.getText(), tfFormula.getText(), CBCFormulaItem.CBCType.DEADLOCK,
								checking);
	}
	
	@FXML
	public void checkFormula() {
		addFormula(true);
		cbcHandler.checkDeadlock(tfFormula.getText());
	}
	
	@FXML
	public void cancel() {
		this.close();
	}
		
}
