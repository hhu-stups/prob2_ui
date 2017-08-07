package de.prob2.ui.verifications.cbc;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.project.machines.Machine;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;

import javax.inject.Inject;

import com.google.inject.Injector;

public class CBCDeadlock extends Stage {
		
	private final Injector injector;
	
	private final CBCChecker cbcChecker;
	
	@FXML
	private TextField tfFormula;

	@Inject
	private CBCDeadlock(final StageManager stageManager, final CBCChecker cbcChecker, final Injector injector) {
		this.cbcChecker = cbcChecker;
		this.injector = injector;
		stageManager.loadFXML(this, "cbc_deadlock.fxml");
		this.initModality(Modality.APPLICATION_MODAL);
	}
	
	@FXML
	public void addFormula() {
		CBCFormulaItem formula = new CBCFormulaItem(tfFormula.getText(), tfFormula.getText(), CBCFormulaItem.CBCType.DEADLOCK);
		Machine currentMachine = injector.getInstance(CBCView.class).getCurrentMachine();
		if(currentMachine != null) {
			if(!currentMachine.getCBCFormulas().contains(formula)) {
				currentMachine.addCBCFormula(formula);
			}
		}
		injector.getInstance(CBCView.class).updateProject();
		this.close();
	}
	
	@FXML
	public void checkFormula() {
		addFormula();
		cbcChecker.checkDeadlock(tfFormula.getText());
	}
	
	@FXML
	public void cancelFormula() {
		this.close();
	}
		
}
