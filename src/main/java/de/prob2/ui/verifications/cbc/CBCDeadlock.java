package de.prob2.ui.verifications.cbc;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.AbstractResultHandler.ItemType;
import de.prob2.ui.verifications.cbc.CBCFormulaItem.CBCType;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;

import javax.inject.Inject;

import com.google.inject.Injector;

public class CBCDeadlock extends Stage {
	
	private final CBCFormulaHandler cbcHandler;
	
	@FXML
	private TextField tfFormula;
	
	@FXML
	private Button btAdd;
	
	@FXML
	private Button btCheck;
	
	private final Injector injector;

	@Inject
	private CBCDeadlock(final StageManager stageManager, final CBCFormulaHandler cbcHandler,
						final Injector injector) {
		this.cbcHandler = cbcHandler;
		this.injector = injector;
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
		btAdd.setText("Change");
		btAdd.setOnAction(e-> {
			if(!updateFormula(item)) {
				injector.getInstance(CBCResultHandler.class).showAlreadyExists(ItemType.Formula);
			}
			this.close();
		});
		btCheck.setText("Change and Check");
		btCheck.setOnAction(e-> {
			if(updateFormula(item)) {
				cbcHandler.checkDeadlock(item.getCode());
			} else {
				injector.getInstance(CBCResultHandler.class).showAlreadyExists(ItemType.Formula);
			}
			this.close();
		});
		tfFormula.setText(item.getCode());
		this.showAndWait();
	}
	
	private boolean updateFormula(CBCFormulaItem item) {
		Machine currentMachine = injector.getInstance(CBCView.class).getCurrentMachine();
		String formula = tfFormula.getText();
		CBCFormulaItem newItem = new CBCFormulaItem(formula, formula, CBCType.DEADLOCK);
		if(!currentMachine.getCBCFormulas().contains(newItem)) {
			item.setName(formula);
			item.setCode(formula);
			item.reset();
			injector.getInstance(CBCView.class).refresh();
			return true;
		}	
		return false;
	}
	
	@FXML
	public void cancel() {
		this.close();
	}
		
}
