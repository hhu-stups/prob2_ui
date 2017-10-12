package de.prob2.ui.verifications.symbolicchecking;


import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import com.google.inject.Injector;

import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.AbstractResultHandler;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Control;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public abstract class AbstractSymbolicCheckingFormulaInputStage extends Stage {
	

	protected final SymbolicCheckingFormulaHandler symbolicCheckingHandler;
	
	protected final CurrentProject currentProject;
	
	@FXML
	protected Button btAdd;
	
	@FXML
	protected Button btCheck;
	
	protected final Injector injector;
	
	protected final ResourceBundle bundle;
	
	protected final List<Button> invisibles;
	
	public AbstractSymbolicCheckingFormulaInputStage(final SymbolicCheckingFormulaHandler cbcHandler, final CurrentProject currentProject, final Injector injector, final ResourceBundle bundle) {
		this.symbolicCheckingHandler = cbcHandler;
		this.currentProject = currentProject;
		this.injector = injector;
		this.bundle = bundle;
		this.invisibles = new ArrayList<>();
	}
	
	@SuppressWarnings("unchecked")
	protected void changeFormula(Control input, SymbolicCheckingFormulaItem item, List<Button> invisibles) {
		hideInvisibleButtons(invisibles);
		btAdd.setText(bundle.getString("verifications.symbolic.input.change"));
		btAdd.setOnAction(e-> {
			if(!updateFormula(input, item)) {
				injector.getInstance(SymbolicCheckingResultHandler.class).showAlreadyExists(AbstractResultHandler.ItemType.FORMULA);
			}
			this.close();
		});

		btCheck.setText(bundle.getString("verifications.symbolic.input.changeAndCheck"));
		btCheck.setOnAction(e-> {
			if(updateFormula(input, item)) {
				symbolicCheckingHandler.checkItem(item);
			} else {
				injector.getInstance(SymbolicCheckingResultHandler.class).showAlreadyExists(AbstractResultHandler.ItemType.FORMULA);
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
	
	private void hideInvisibleButtons(List<Button> invisibles) {
		for(Button button : invisibles) {
			button.setVisible(false);
		}
	}

	@SuppressWarnings("unchecked")
	private boolean updateFormula(Control input, SymbolicCheckingFormulaItem item) {
		Machine currentMachine = currentProject.getCurrentMachine();
		String formula;
		if(input instanceof TextField) {
			formula = ((TextField) input).getText();
		} else {
			formula = ((ChoiceBox<String>) input).getSelectionModel().getSelectedItem();
		}
		SymbolicCheckingFormulaItem newItem = new SymbolicCheckingFormulaItem(formula, formula, item.getType());
		if(!currentMachine.getSymbolicCheckingFormulas().contains(newItem)) {
			item.setName(formula);
			item.setCode(formula);
			item.reset();
			injector.getInstance(SymbolicCheckingView.class).refresh();
			return true;
		}	
		return false;
	}

}
