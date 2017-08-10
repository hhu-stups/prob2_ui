package de.prob2.ui.verifications.cbc;

import java.util.ArrayList;

import javax.inject.Inject;

import de.prob.model.representation.AbstractElement;
import de.prob.model.representation.BEvent;
import de.prob.statespace.Trace;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class CBCInvariants extends Stage {

	@FXML
	private ChoiceBox<String> cbOperations;

	private final CurrentTrace currentTrace;

	private final CBCFormulaHandler cbcHandler;

	@Inject
	private CBCInvariants(final StageManager stageManager, final CurrentTrace currentTrace, 
			final CBCFormulaHandler cbcHandler) {
		this.currentTrace = currentTrace;
		this.cbcHandler = cbcHandler;
		stageManager.loadFXML(this, "cbc_invariants.fxml");
		this.initModality(Modality.APPLICATION_MODAL);
	}

	@FXML
	public void initialize() {
		this.update(currentTrace.get());
		currentTrace.addListener((observable, from, to) -> update(to));
	}

	private void update(Trace trace) {
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
		String item = cbOperations.getSelectionModel().getSelectedItem();
		if (item == null) {
			return;
		}
		cbcHandler.addFormula(item, "", CBCFormulaItem.CBCType.INVARIANT);
		this.close();
	}

	@FXML
	public void checkFormula() {
		addFormula();
		String name = cbOperations.getSelectionModel().getSelectedItem();
		if (name == null) {
			return;
		}
		cbcHandler.checkInvariant(name);
	}

	@FXML
	public void cancelFormula() {
		this.close();
	}

}
