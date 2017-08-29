package de.prob2.ui.verifications.cbc;

import de.prob2.ui.internal.StageManager;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert.AlertType;
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
		if("FIND DEADLOCK".equals(tfFormula.getText())) {
			showInvalidFormula();
			return;
		}
		cbcHandler.addFormula(tfFormula.getText(), tfFormula.getText(), CBCFormulaItem.CBCType.DEADLOCK,
								checking);
		this.close();
	}
	
	@FXML
	public void checkFormula() {
		if("FIND DEADLOCK".equals(tfFormula.getText())) {
			showInvalidFormula();
			return;
		}
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
	
	public void showInvalidFormula() {
		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setTitle("Invalid Formula");
		alert.setHeaderText("Invalid Formula");
		alert.setContentText("Formula is valid!");
		alert.showAndWait();
	}
		
}
