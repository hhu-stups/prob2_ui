package de.prob2.ui.verifications.cbc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import com.google.inject.Injector;

import de.prob.model.representation.AbstractElement;
import de.prob.model.representation.BEvent;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.stage.Modality;

public class CBCInvariants extends AbstractCBCFormulaInputStage {

	@FXML
	private ChoiceBox<String> cbOperations;
	
	@FXML
	private Button checkAllOperationsButton;
	
	@FXML
	private Button findRedundantsButton;
	
	private final CurrentTrace currentTrace;
	
	private ArrayList<String> events;
	
	@Inject
	private CBCInvariants(final StageManager stageManager, final CurrentTrace currentTrace, 
							final CBCFormulaHandler cbcHandler, final Injector injector) {
		super(cbcHandler, injector);
		this.currentTrace = currentTrace;
		this.events = new ArrayList<>();
		stageManager.loadFXML(this, "cbc_invariants.fxml");
		this.initModality(Modality.APPLICATION_MODAL);
	}

	@FXML
	public void initialize() {
		this.update();
		this.invisibles.addAll(Arrays.asList(checkAllOperationsButton, findRedundantsButton));
		currentTrace.addListener((observable, from, to) -> update());
	}

	private void update() {
		events.clear();
		if (currentTrace.get() != null) {
			AbstractElement mainComponent = currentTrace.getStateSpace().getMainComponent();
			if (mainComponent instanceof de.prob.model.representation.Machine) {
				for (BEvent e : mainComponent.getChildrenOfType(BEvent.class)) {
					events.add(e.getName());
				}
			}
			cbOperations.getItems().setAll(events);
		}
	}
	
	public List<String> getEvents() {
		return events;
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
	
	@FXML
	public void checkAllOperations() {
		List<String> events = injector.getInstance(CBCInvariants.class).getEvents();
		for(String event : events) {
			cbcHandler.addFormula(event, event, CBCFormulaItem.CBCType.INVARIANT, true);
			cbcHandler.checkInvariant(event);
		}
		this.close();
	}
	
	@FXML
	public void findRedundants() {
		cbcHandler.addFormula("FIND REDUNDANT INVARIANTS", "FIND REDUNDANT INVARIANTS", CBCFormulaItem.CBCType.INVARIANT, true);
		cbcHandler.findRedundantInvariants();
	}
	
	public void changeFormula(CBCFormulaItem item) {
		super.changeFormula(cbOperations, item, invisibles);
	}
	
	@FXML
	public void cancel() {
		this.close();
	}

}
