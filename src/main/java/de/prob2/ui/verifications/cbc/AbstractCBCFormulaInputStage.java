package de.prob2.ui.verifications.cbc;


import com.google.inject.Injector;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.AbstractResultHandler.ItemType;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Control;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public abstract class AbstractCBCFormulaInputStage extends Stage {
	

	protected final CBCFormulaHandler cbcHandler;
	
	@FXML
	protected Button btAdd;
	
	@FXML
	protected Button btCheck;
	
	protected final Injector injector;
	
	public AbstractCBCFormulaInputStage(final CBCFormulaHandler cbcHandler, final Injector injector) {
		this.cbcHandler = cbcHandler;
		this.injector = injector;
		
	}
	
	@SuppressWarnings("unchecked")
	protected void changeFormula(Control input, CBCFormulaItem item) {
		btAdd.setText("Change");
		btAdd.setOnAction(e-> {
			if(!updateFormula(input, item)) {
				injector.getInstance(CBCResultHandler.class).showAlreadyExists(ItemType.Formula);
			}
			this.close();
		});

		btCheck.setText("Change and Check");
		btCheck.setOnAction(e-> {
			if(updateFormula(input, item)) {
				cbcHandler.checkItem(item);
			} else {
				injector.getInstance(CBCResultHandler.class).showAlreadyExists(ItemType.Formula);
			}
			this.close();
		});
		if(input instanceof TextField) {
			((TextField) input).setText(item.getCode());
		} else {
			((ChoiceBox<String>) input).getItems().forEach(operationItem -> {
				if(operationItem.equals(item.getCode())) {
					((ChoiceBox<String>) input).getSelectionModel().select(operationItem);
					return;
				}
			});
		}
		this.showAndWait();
	}

	@SuppressWarnings("unchecked")
	private boolean updateFormula(Control input, CBCFormulaItem item) {
		Machine currentMachine = injector.getInstance(CBCView.class).getCurrentMachine();
		String formula;
		if(input instanceof TextField) {
			formula = ((TextField) input).getText();
		} else {
			formula = ((ChoiceBox<String>) input).getSelectionModel().getSelectedItem();
		}
		CBCFormulaItem newItem = new CBCFormulaItem(formula, formula, item.getType());
		if(!currentMachine.getCBCFormulas().contains(newItem)) {
			item.setName(formula);
			item.setCode(formula);
			item.reset();
			injector.getInstance(CBCView.class).refresh();
			return true;
		}	
		return false;
	}

}
