package de.prob2.ui.verifications.cbc;

import java.util.ArrayList;

import javax.inject.Inject;

import com.google.inject.Injector;

import de.prob.model.representation.AbstractElement;
import de.prob.model.representation.BEvent;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.AbstractResultHandler.ItemType;
import de.prob2.ui.verifications.cbc.CBCFormulaItem.CBCType;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class CBCInvariants extends Stage {

	@FXML
	private ChoiceBox<String> cbOperations;
	
	@FXML
	private Button btAdd;
	
	@FXML
	private Button btCheck;

	private final CurrentTrace currentTrace;

	private final CBCFormulaHandler cbcHandler;
	
	private final Injector injector;

	@Inject
	private CBCInvariants(final StageManager stageManager, final CurrentTrace currentTrace, 
			final CBCFormulaHandler cbcHandler, final Injector injector) {
		this.currentTrace = currentTrace;
		this.cbcHandler = cbcHandler;
		this.injector = injector;
		stageManager.loadFXML(this, "cbc_invariants.fxml");
		this.initModality(Modality.APPLICATION_MODAL);
	}

	@FXML
	public void initialize() {
		this.update();
		currentTrace.addListener((observable, from, to) -> update());
	}

	private void update() {
		if (currentTrace.get() != null) {
			ArrayList<String> events = new ArrayList<>();
			AbstractElement mainComponent = currentTrace.getStateSpace().getMainComponent();
			if (mainComponent instanceof de.prob.model.representation.Machine) {
				for (BEvent e : mainComponent.getChildrenOfType(BEvent.class)) {
					events.add(e.getName());
				}
			}
			cbOperations.getItems().setAll(events);
		}
	}

	@FXML
	public void addFormula() {
		addFormula(false);
	}
	
	private void addFormula(boolean checking) {
		String item = cbOperations.getSelectionModel().getSelectedItem();
		if (item == null) {
			return;
		}
		cbcHandler.addFormula(item, item, CBCFormulaItem.CBCType.INVARIANT, checking);
		this.close();
	}

	@FXML
	public void checkFormula() {
		addFormula(true);
		String code = cbOperations.getSelectionModel().getSelectedItem();
		if (code == null) {
			return;
		}
		cbcHandler.checkInvariant(code);
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
				cbcHandler.checkInvariant(item.getCode());
			} else {
				injector.getInstance(CBCResultHandler.class).showAlreadyExists(ItemType.Formula);
			}
			this.close();
		});
		cbOperations.getItems().forEach(operationItem -> {
			if(operationItem.equals(item.getCode())) {
				cbOperations.getSelectionModel().select(operationItem);
				return;
			}
		});
		this.showAndWait();
	}

	private boolean updateFormula(CBCFormulaItem item) {
		Machine currentMachine = injector.getInstance(CBCView.class).getCurrentMachine();
		String formula = cbOperations.getSelectionModel().getSelectedItem();
		CBCFormulaItem newItem = new CBCFormulaItem(formula, formula, CBCType.INVARIANT);
		if(!currentMachine.getCBCFormulas().contains(newItem)) {
			item.setName(formula);
			item.setCode(formula);
			item.initializeStatus();
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
