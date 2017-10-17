package de.prob2.ui.verifications.symbolicchecking;


import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.model.representation.AbstractElement;
import de.prob.model.representation.BEvent;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.AbstractResultHandler;
import de.prob2.ui.verifications.symbolicchecking.SymbolicCheckingFormulaItem.SymbolicCheckingType;
import de.prob2.ui.verifications.symbolicchecking.SymbolicCheckingItem.CheckingType;
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
		CheckingType checkingType = injector.getInstance(SymbolicCheckingChoosingStage.class).getCheckingType();
		SymbolicCheckingFormulaItem formulaItem;
		switch(checkingType) {
			case INVARIANTS:
				String item = cbOperations.getSelectionModel().getSelectedItem();
				symbolicCheckingHandler.addFormula(item, item, SymbolicCheckingFormulaItem.SymbolicCheckingType.INVARIANT, checking);
				break;
			case DEADLOCK:
				symbolicCheckingHandler.addFormula(tfFormula.getText(), tfFormula.getText(), SymbolicCheckingFormulaItem.SymbolicCheckingType.DEADLOCK,
						checking);
				break;
			case SEQUENCE:
				symbolicCheckingHandler.addFormula(tfFormula.getText(), tfFormula.getText(), SymbolicCheckingFormulaItem.SymbolicCheckingType.SEQUENCE,
						checking);
				break;
			case CHECK_ALL_OPERATIONS:
				for(String event : events) {
					symbolicCheckingHandler.addFormula(event, event, SymbolicCheckingFormulaItem.SymbolicCheckingType.INVARIANT, true);
				}
				break;
			case FIND_REDUNDANT_INVARIANTS:
				formulaItem = new SymbolicCheckingFormulaItem(FIND_REDUNDANT_INVARIANTS, FIND_REDUNDANT_INVARIANTS, SymbolicCheckingFormulaItem.SymbolicCheckingType.FIND_REDUNDANT_INVARIANTS);
				symbolicCheckingHandler.addFormula(formulaItem, true);
				break;
			case FIND_DEADLOCK:
				symbolicCheckingHandler.addFormula(FIND_DEADLOCK, FIND_DEADLOCK, SymbolicCheckingFormulaItem.SymbolicCheckingType.FIND_DEADLOCK, true);
				break;
			case FIND_VALID_STATE:
				formulaItem = new SymbolicCheckingFormulaItem(tfFormula.getText(), tfFormula.getText(), 
						SymbolicCheckingType.FIND_VALID_STATE);
				symbolicCheckingHandler.addFormula(formulaItem, true);
				break;
			case CHECK_ASSERTIONS:
				formulaItem = new SymbolicCheckingFormulaItem(ASSERTION_CHECKING, ASSERTION_CHECKING, SymbolicCheckingFormulaItem.SymbolicCheckingType.ASSERTIONS);
				symbolicCheckingHandler.addFormula(formulaItem, true);
				break;
			case CHECK_REFINEMENT:
				formulaItem = new SymbolicCheckingFormulaItem(REFINEMENT_CHECKING, REFINEMENT_CHECKING, SymbolicCheckingFormulaItem.SymbolicCheckingType.REFINEMENT);
				symbolicCheckingHandler.addFormula(formulaItem, true);
				break;
			default:
				break;
		}
		injector.getInstance(SymbolicCheckingChoosingStage.class).close();
	}
	
	@FXML
	public void checkFormula() {
		CheckingType checkingType = injector.getInstance(SymbolicCheckingChoosingStage.class).getCheckingType();
		SymbolicCheckingFormulaItem formulaItem;
		switch(checkingType) {
			case INVARIANTS:
				addFormula(true);
				symbolicCheckingHandler.checkInvariant(cbOperations.getSelectionModel().getSelectedItem());
				break;
			case DEADLOCK:
				addFormula(true);
				symbolicCheckingHandler.checkDeadlock(tfFormula.getText());
				break;
			case SEQUENCE:
				addFormula(true);
				symbolicCheckingHandler.checkSequence(tfFormula.getText());
				break;
			case CHECK_ALL_OPERATIONS:
				for(String event : events) {
					symbolicCheckingHandler.checkInvariant(event);
				}
				break;
			case FIND_REDUNDANT_INVARIANTS:
				formulaItem = new SymbolicCheckingFormulaItem(FIND_REDUNDANT_INVARIANTS, FIND_REDUNDANT_INVARIANTS, SymbolicCheckingFormulaItem.SymbolicCheckingType.FIND_REDUNDANT_INVARIANTS);
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
				formulaItem = new SymbolicCheckingFormulaItem(ASSERTION_CHECKING, ASSERTION_CHECKING, SymbolicCheckingFormulaItem.SymbolicCheckingType.ASSERTIONS);
				symbolicCheckingHandler.checkAssertions(formulaItem);
				break;
			case CHECK_REFINEMENT:
				formulaItem = new SymbolicCheckingFormulaItem(REFINEMENT_CHECKING, REFINEMENT_CHECKING, SymbolicCheckingFormulaItem.SymbolicCheckingType.REFINEMENT);
				symbolicCheckingHandler.checkRefinement(formulaItem);
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
