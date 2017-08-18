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

public class CBCSequence extends Stage {

	@FXML
	private TextField tfSequence;
	
	@FXML
	private Button btAdd;
	
	@FXML
	private Button btCheck;
	
	private final CBCFormulaHandler cbcHandler;
	
	private final Injector injector;
	
	@Inject
	private CBCSequence(final StageManager stageManager, final CBCFormulaHandler cbcHandler,
						final Injector injector) {
		this.cbcHandler = cbcHandler;
		this.injector = injector;
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
		this.close();
	}

	@FXML
	public void checkFormula() {
		addFormula(true);
		cbcHandler.checkSequence(tfSequence.getText());
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
				cbcHandler.checkSequence(item.getCode());
			} else {
				injector.getInstance(CBCResultHandler.class).showAlreadyExists(ItemType.Formula);
			}
			this.close();
		});
		tfSequence.setText(item.getCode());
		this.showAndWait();
	}
	
	private boolean updateFormula(CBCFormulaItem item) {
		Machine currentMachine = injector.getInstance(CBCView.class).getCurrentMachine();
		String formula = tfSequence.getText();
		CBCFormulaItem newItem = new CBCFormulaItem(formula, formula, CBCType.SEQUENCE);
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
