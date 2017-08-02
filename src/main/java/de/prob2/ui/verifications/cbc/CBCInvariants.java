package de.prob2.ui.verifications.cbc;

import java.util.ArrayList;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Injector;

import de.prob.check.CBCInvariantChecker;

import de.prob.model.representation.AbstractElement;
import de.prob.model.representation.BEvent;
import de.prob.statespace.Trace;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class CBCInvariants extends Stage {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(CBCInvariants.class);
	
	@FXML
	private ChoiceBox<String> cbOperations;
	
	private final CurrentTrace currentTrace;
	
	private final Injector injector;
	
	@Inject
	private CBCInvariants(final StageManager stageManager, final CurrentTrace currentTrace, 
							final Injector injector) {
		this.currentTrace = currentTrace;
		this.injector = injector;
		stageManager.loadFXML(this, "cbc_invariants.fxml");
		this.initModality(Modality.APPLICATION_MODAL);	
	}
	
	@FXML
	public void initialize() {
		this.update(currentTrace.get());
		currentTrace.addListener((observable, from, to)-> {
			update(to);
		});
	}
	
	private void update(Trace trace) {
		if(currentTrace.get() != null) {
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
		ArrayList<String> event = new ArrayList<>();
		event.add(cbOperations.getSelectionModel().getSelectedItem().toString());
		String name = cbOperations.getSelectionModel().getSelectedItem().toString();
		CBCFormulaItem formula = new CBCFormulaItem(name, "INVARIANT", "");
		Machine currentMachine = injector.getInstance(CBCView.class).getCurrentMachine();
		if(currentMachine != null) {
			currentMachine.addCBCFormula(formula);
		}
		this.close();
	}
	
	@FXML
	public void checkFormula() {
		addFormula();
		ArrayList<String> event = new ArrayList<>();
		event.add(cbOperations.getSelectionModel().getSelectedItem().toString());
		CBCInvariantChecker checker = new CBCInvariantChecker(currentTrace.getStateSpace(), event);
		try {
			System.out.println(checker.call());
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
		}
	}
	
	@FXML
	public void cancelFormula() {
		this.close();
	}
	
	
}
