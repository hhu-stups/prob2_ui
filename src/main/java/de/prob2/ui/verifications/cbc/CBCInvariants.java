package de.prob2.ui.verifications.cbc;

import java.util.ArrayList;

import javax.inject.Inject;

import com.google.inject.Injector;

import de.prob.model.representation.AbstractElement;
import de.prob.model.representation.BEvent;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.stage.Modality;

public class CBCInvariants extends AbstractCBCFormulaInputStage {

	@FXML
	private ChoiceBox<String> cbOperations;

	private final CurrentTrace currentTrace;

	@Inject
	private CBCInvariants(final StageManager stageManager, final CurrentTrace currentTrace, 
							final CBCFormulaHandler cbcHandler, final Injector injector) {
		super(cbcHandler, injector);
		this.currentTrace = currentTrace;
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
		super.changeFormula(cbOperations, item);
	}
	
	@FXML
	public void cancel() {
		this.close();
	}

}
