package de.prob2.ui.verifications.cbc;

import de.prob2.ui.internal.StageManager;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.Modality;

import javax.inject.Inject;

import com.google.inject.Injector;

public class CBCDeadlock extends AbstractCBCFormulaInputStage {
		
	@FXML
	private TextField tfFormula;
	

	@Inject
	private CBCDeadlock(final StageManager stageManager, final CBCFormulaHandler cbcHandler,
						final Injector injector) {
		super(cbcHandler, injector);
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
		this.close();
	}
	
	@FXML
	public void checkFormula() {
		addFormula(true);
		cbcHandler.checkDeadlock(tfFormula.getText());
		this.close();
	}
	
	public void changeFormula(CBCFormulaItem item) {
		super.changeFormula(tfFormula, item);
	}
	
	
	@FXML
	public void cancel() {
		this.close();
	}
		
}
