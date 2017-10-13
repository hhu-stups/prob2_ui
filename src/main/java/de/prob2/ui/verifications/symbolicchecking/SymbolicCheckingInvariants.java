package de.prob2.ui.verifications.symbolicchecking;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

import javax.inject.Inject;

import com.google.inject.Injector;

import de.prob.model.representation.AbstractElement;
import de.prob.model.representation.BEvent;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.stage.Modality;

public class SymbolicCheckingInvariants extends AbstractSymbolicCheckingFormulaInputStage {

	@FXML
	private ChoiceBox<String> cbOperations;
	
	@FXML
	private Button checkAllOperationsButton;
	
	@FXML
	private Button findRedundantsButton;
	
	private final CurrentTrace currentTrace;
	
	private ArrayList<String> events;
	
	@Inject
	private SymbolicCheckingInvariants(final StageManager stageManager, final CurrentTrace currentTrace, final CurrentProject currentProject, 
							final SymbolicCheckingFormulaHandler cbcHandler, final Injector injector, final ResourceBundle bundle) {
		super(cbcHandler, currentProject, injector, bundle);
		this.currentTrace = currentTrace;
		this.events = new ArrayList<>();
		stageManager.loadFXML(this, "symbolic_checking_invariants.fxml");
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
		symbolicCheckingHandler.addFormula(item, item, SymbolicCheckingFormulaItem.SymbolicCheckingType.INVARIANT, checking);
		this.close();
	}

	@FXML
	public void checkFormula() {
		addFormula(true);
		String code = cbOperations.getSelectionModel().getSelectedItem();
		if (code == null) {
			return;
		}
		symbolicCheckingHandler.checkInvariant(code);
		this.close();
	}
	
	@FXML
	public void checkAllOperations() {
		for(String event : events) {
			symbolicCheckingHandler.addFormula(event, event, SymbolicCheckingFormulaItem.SymbolicCheckingType.INVARIANT, true);
			symbolicCheckingHandler.checkInvariant(event);
		}
		this.close();
	}
	
	@FXML
	public void findRedundants() {
		SymbolicCheckingFormulaItem item = new SymbolicCheckingFormulaItem("FIND REDUNDANT INVARIANTS", "FIND REDUNDANT INVARIANTS", SymbolicCheckingFormulaItem.SymbolicCheckingType.FIND_REDUNDANT_INVARIANTS);
		symbolicCheckingHandler.addFormula(item, true);
		symbolicCheckingHandler.findRedundantInvariants(item);
		this.close();
	}
	
	public void changeFormula(SymbolicCheckingFormulaItem item) {
		super.changeFormula(cbOperations, item, invisibles);
	}
	
	@FXML
	public void cancel() {
		this.close();
	}

}
