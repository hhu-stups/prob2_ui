package de.prob2.ui.verifications.symbolicchecking;


import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.animator.command.SymbolicModelcheckCommand;
import de.prob.model.representation.AbstractElement;
import de.prob.model.representation.BEvent;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.AbstractResultHandler;
import de.prob2.ui.verifications.symbolicchecking.SymbolicCheckingItem.GUIType;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;

@Singleton
public class SymbolicCheckingFormulaInput extends StackPane {
	
	private final SymbolicCheckingFormulaHandler symbolicCheckingHandler;
	
	private final CurrentProject currentProject;
	
	private static final String FIND_REDUNDANT_INVARIANTS = "FIND REDUNDANT INVARIANTS";
	
	private static final String FIND_DEADLOCK = "FIND DEADLOCK";
	
	private static final String ASSERTION_CHECKING = "Assertion Checking";
	
	private static final String REFINEMENT_CHECKING = "Refinement Checking";
	
	private static final String IC3 = "IC3";
	
	private static final String TINDUCTION = "TINDUCTION";
	
	private static final String KINDUCTION = "KINDUCTION";
	
	private static final String BMC = "BMC";
	
	@FXML
	private Button btAdd;
	
	@FXML
	private Button btCheck;
	
	@FXML
	private TextField tfFormula;
	
	@FXML
	private ChoiceBox<String> cbOperations;
	
	private final Injector injector;
	
	private final ResourceBundle bundle;
	
	private final CurrentTrace currentTrace;
	
	private ArrayList<String> events;
	
	@Inject
	public SymbolicCheckingFormulaInput(final StageManager stageManager, final SymbolicCheckingFormulaHandler cbcHandler, 
										final CurrentProject currentProject, final Injector injector, final ResourceBundle bundle,
										final CurrentTrace currentTrace) {
		this.symbolicCheckingHandler = cbcHandler;
		this.currentProject = currentProject;
		this.currentTrace = currentTrace;
		this.events = new ArrayList<>();
		this.injector = injector;
		this.bundle = bundle;
		stageManager.loadFXML(this, "symbolic_checking_formula_input.fxml");
	}
	
	@FXML
	public void initialize() {
		this.update();
		currentTrace.addListener((observable, from, to) -> update());
	}
	
	public void changeFormula(SymbolicCheckingFormulaItem item) {
		btAdd.setText(bundle.getString("verifications.symbolic.input.change"));
		btAdd.setOnAction(e-> {
			if(!updateFormula(item)) {
				injector.getInstance(SymbolicCheckingResultHandler.class).showAlreadyExists(AbstractResultHandler.ItemType.FORMULA);
			}
			injector.getInstance(SymbolicCheckingChoosingStage.class).close();
		});

		btCheck.setText(bundle.getString("verifications.symbolic.input.changeAndCheck"));
		btCheck.setOnAction(e-> {
			if(updateFormula(item)) {
				symbolicCheckingHandler.checkItem(item);
			} else {
				injector.getInstance(SymbolicCheckingResultHandler.class).showAlreadyExists(AbstractResultHandler.ItemType.FORMULA);
			}
			injector.getInstance(SymbolicCheckingChoosingStage.class).close();
		});
		SymbolicCheckingChoosingStage choosingStage = injector.getInstance(SymbolicCheckingChoosingStage.class);
		if(choosingStage.getGUIType() == GUIType.TEXT_FIELD) {
			tfFormula.setText(item.getCode());
		} else if(choosingStage.getGUIType() == GUIType.CHOICE_BOX) {
			cbOperations.getItems().forEach(operationItem -> {
				if(operationItem.equals(item.getCode())) {
					cbOperations.getSelectionModel().select(operationItem);
					return;
				}
			});
		}
		injector.getInstance(SymbolicCheckingChoosingStage.class).showAndWait();
	}

	private boolean updateFormula(SymbolicCheckingFormulaItem item) {
		Machine currentMachine = currentProject.getCurrentMachine();
		String formula = null;
		SymbolicCheckingChoosingStage choosingStage = injector.getInstance(SymbolicCheckingChoosingStage.class);
		if(choosingStage.getGUIType() == GUIType.TEXT_FIELD) {
			formula = tfFormula.getText();
		} else if(choosingStage.getGUIType() == GUIType.CHOICE_BOX) {
			formula = cbOperations.getSelectionModel().getSelectedItem();
		} else {
			formula = choosingStage.getCheckingType().name();
		}
		SymbolicCheckingFormulaItem newItem = new SymbolicCheckingFormulaItem(formula, formula, item.getType());
		if(!currentMachine.getSymbolicCheckingFormulas().contains(newItem)) {
			SymbolicCheckingType type = choosingStage.getCheckingType();
			item.setType(type);
			item.setDescription(type.name());
			item.setName(formula);
			item.setCode(formula);
			item.reset();
			injector.getInstance(SymbolicCheckingView.class).refresh();
			return true;
		}	
		return false;
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
		SymbolicCheckingType checkingType = injector.getInstance(SymbolicCheckingChoosingStage.class).getCheckingType();
		SymbolicCheckingFormulaItem formulaItem;
		switch(checkingType) {
			case INVARIANT:
				String item = cbOperations.getSelectionModel().getSelectedItem();
				symbolicCheckingHandler.addFormula(item, item, SymbolicCheckingType.INVARIANT, checking);
				break;
			case DEADLOCK:
				symbolicCheckingHandler.addFormula(tfFormula.getText(), tfFormula.getText(), SymbolicCheckingType.DEADLOCK,
						checking);
				break;
			case SEQUENCE:
				symbolicCheckingHandler.addFormula(tfFormula.getText(), tfFormula.getText(), SymbolicCheckingType.SEQUENCE,
						checking);
				break;
			case CHECK_ALL_OPERATIONS:
				for(String event : events) {
					symbolicCheckingHandler.addFormula(event, event, SymbolicCheckingType.INVARIANT, checking);
				}
				break;
			case FIND_REDUNDANT_INVARIANTS:
				formulaItem = new SymbolicCheckingFormulaItem(FIND_REDUNDANT_INVARIANTS, FIND_REDUNDANT_INVARIANTS, SymbolicCheckingType.FIND_REDUNDANT_INVARIANTS);
				symbolicCheckingHandler.addFormula(formulaItem, checking);
				break;
			case FIND_DEADLOCK:
				symbolicCheckingHandler.addFormula(FIND_DEADLOCK, FIND_DEADLOCK, SymbolicCheckingType.FIND_DEADLOCK, checking);
				break;
			case FIND_VALID_STATE:
				formulaItem = new SymbolicCheckingFormulaItem(tfFormula.getText(), tfFormula.getText(), 
						SymbolicCheckingType.FIND_VALID_STATE);
				symbolicCheckingHandler.addFormula(formulaItem, checking);
				break;
			case CHECK_ASSERTIONS:
				formulaItem = new SymbolicCheckingFormulaItem(ASSERTION_CHECKING, ASSERTION_CHECKING, SymbolicCheckingType.ASSERTIONS);
				symbolicCheckingHandler.addFormula(formulaItem, checking);
				break;
			case CHECK_REFINEMENT:
				formulaItem = new SymbolicCheckingFormulaItem(REFINEMENT_CHECKING, REFINEMENT_CHECKING, SymbolicCheckingType.REFINEMENT);
				symbolicCheckingHandler.addFormula(formulaItem, checking);
				break;
			case IC3:
				formulaItem = new SymbolicCheckingFormulaItem(IC3, IC3, SymbolicCheckingType.IC3);
				symbolicCheckingHandler.addFormula(formulaItem, checking);
				break;
			case TINDUCTION:
				formulaItem = new SymbolicCheckingFormulaItem(TINDUCTION, TINDUCTION, SymbolicCheckingType.TINDUCTION);
				symbolicCheckingHandler.addFormula(formulaItem, checking);
				break;
			case KINDUCTION:
				formulaItem = new SymbolicCheckingFormulaItem(KINDUCTION, KINDUCTION, SymbolicCheckingType.KINDUCTION);
				symbolicCheckingHandler.addFormula(formulaItem, checking);
				break;
			case BMC:
				formulaItem = new SymbolicCheckingFormulaItem(BMC, BMC, SymbolicCheckingType.BMC);
				symbolicCheckingHandler.addFormula(formulaItem, checking);
				break;
			default:
				break;
		}
		injector.getInstance(SymbolicCheckingChoosingStage.class).close();
	}
	
	@FXML
	public void checkFormula() {
		SymbolicCheckingType checkingType = injector.getInstance(SymbolicCheckingChoosingStage.class).getCheckingType();
		SymbolicCheckingFormulaItem formulaItem;
		addFormula(true);
		switch(checkingType) {
			case INVARIANT:
				symbolicCheckingHandler.checkInvariant(cbOperations.getSelectionModel().getSelectedItem());
				break;
			case DEADLOCK:
				symbolicCheckingHandler.checkDeadlock(tfFormula.getText());
				break;
			case SEQUENCE:
				symbolicCheckingHandler.checkSequence(tfFormula.getText());
				break;
			case CHECK_ALL_OPERATIONS:
				for(String event : events) {
					symbolicCheckingHandler.checkInvariant(event);
				}
				break;
			case FIND_REDUNDANT_INVARIANTS:
				formulaItem = new SymbolicCheckingFormulaItem(FIND_REDUNDANT_INVARIANTS, FIND_REDUNDANT_INVARIANTS, SymbolicCheckingType.FIND_REDUNDANT_INVARIANTS);
				symbolicCheckingHandler.findRedundantInvariants(formulaItem);
				break;
			case FIND_DEADLOCK:
				symbolicCheckingHandler.findDeadlock();
				break;
			case FIND_VALID_STATE:
				formulaItem = new SymbolicCheckingFormulaItem(tfFormula.getText(), tfFormula.getText(), 
						SymbolicCheckingType.FIND_VALID_STATE);
				symbolicCheckingHandler.findValidState(formulaItem);
				break;
			case CHECK_ASSERTIONS:
				formulaItem = new SymbolicCheckingFormulaItem(ASSERTION_CHECKING, ASSERTION_CHECKING, SymbolicCheckingType.ASSERTIONS);
				symbolicCheckingHandler.checkAssertions(formulaItem);
				break;
			case CHECK_REFINEMENT:
				formulaItem = new SymbolicCheckingFormulaItem(REFINEMENT_CHECKING, REFINEMENT_CHECKING, SymbolicCheckingType.REFINEMENT);
				symbolicCheckingHandler.checkRefinement(formulaItem);
				break;
			case IC3:
				formulaItem = new SymbolicCheckingFormulaItem(IC3, IC3, SymbolicCheckingType.IC3);
				symbolicCheckingHandler.checkSymbolic(formulaItem, SymbolicModelcheckCommand.Algorithm.IC3);
				break;
			case TINDUCTION:
				formulaItem = new SymbolicCheckingFormulaItem(TINDUCTION, TINDUCTION, SymbolicCheckingType.TINDUCTION);
				symbolicCheckingHandler.checkSymbolic(formulaItem, SymbolicModelcheckCommand.Algorithm.TINDUCTION);
				break;
			case KINDUCTION:
				formulaItem = new SymbolicCheckingFormulaItem(KINDUCTION, KINDUCTION, SymbolicCheckingType.KINDUCTION);
				symbolicCheckingHandler.checkSymbolic(formulaItem, SymbolicModelcheckCommand.Algorithm.KINDUCTION);
				break;
			case BMC:
				formulaItem = new SymbolicCheckingFormulaItem(BMC, BMC, SymbolicCheckingType.BMC);
				symbolicCheckingHandler.checkSymbolic(formulaItem, SymbolicModelcheckCommand.Algorithm.BMC);
				break;
			default:
				break;
		}
		injector.getInstance(SymbolicCheckingChoosingStage.class).close();
	}
		
	@FXML
	public void cancel() {
		injector.getInstance(SymbolicCheckingChoosingStage.class).close();
	}
	
	public void showTextField() {
		tfFormula.setVisible(true);
		tfFormula.toFront();
		cbOperations.setVisible(false);
	}
	
	public void showChoiceBox() {
		tfFormula.setVisible(false);
		cbOperations.setVisible(true);
		cbOperations.toFront();
	}
	
	public void showNone() {
		tfFormula.setVisible(false);
		cbOperations.setVisible(false);
	}

}
