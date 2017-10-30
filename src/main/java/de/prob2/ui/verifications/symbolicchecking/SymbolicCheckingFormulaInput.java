package de.prob2.ui.verifications.symbolicchecking;


import java.util.ArrayList;
import java.util.EnumMap;
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
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;

@Singleton
public class SymbolicCheckingFormulaInput extends StackPane {
	
	@FunctionalInterface
	private interface EventHandlerOnItem {
		void apply(SymbolicCheckingFormulaItem item);
	}
	
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
	
	private final EventHandlerOnItem changeHandler;
	
	private final EventHandlerOnItem checkAndChangeHandler;
	
	private final EventHandler<ActionEvent> addHandler;
	
	private final EventHandler<ActionEvent> checkHandler;
	
	private final EnumMap<SymbolicCheckingType, String> noneCheckings;
	
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
		noneCheckings = new EnumMap<SymbolicCheckingType, String>(SymbolicCheckingType.class);
		noneCheckings.put(SymbolicCheckingType.FIND_DEADLOCK, FIND_DEADLOCK);
		noneCheckings.put(SymbolicCheckingType.FIND_REDUNDANT_INVARIANTS, FIND_REDUNDANT_INVARIANTS);
		noneCheckings.put(SymbolicCheckingType.CHECK_ASSERTIONS, ASSERTION_CHECKING);
		noneCheckings.put(SymbolicCheckingType.CHECK_REFINEMENT, REFINEMENT_CHECKING);
		noneCheckings.put(SymbolicCheckingType.IC3, IC3);
		noneCheckings.put(SymbolicCheckingType.TINDUCTION, TINDUCTION);
		noneCheckings.put(SymbolicCheckingType.KINDUCTION, KINDUCTION);
		noneCheckings.put(SymbolicCheckingType.BMC, BMC);
		
		changeHandler = item -> 
			btAdd.setOnAction(e -> {
				if(!updateFormula(item)) {
					injector.getInstance(SymbolicCheckingResultHandler.class).showAlreadyExists(AbstractResultHandler.ItemType.FORMULA);
				}
				injector.getInstance(SymbolicCheckingChoosingStage.class).close();
			});
		
		checkAndChangeHandler = item -> 
			btCheck.setOnAction(e-> {
				if(updateFormula(item)) {
					symbolicCheckingHandler.checkItem(item);
				} else {
					injector.getInstance(SymbolicCheckingResultHandler.class).showAlreadyExists(AbstractResultHandler.ItemType.FORMULA);
				}
				injector.getInstance(SymbolicCheckingChoosingStage.class).close();
			});
		
		addHandler = e -> addFormula(false);
		
		checkHandler = e -> {
			SymbolicCheckingType checkingType = injector.getInstance(SymbolicCheckingChoosingStage.class).getCheckingType();
			SymbolicCheckingFormulaItem formulaItem;
			String formula;
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
					formula = noneCheckings.get(SymbolicCheckingType.CHECK_ASSERTIONS);
					formulaItem = new SymbolicCheckingFormulaItem(formula, formula, SymbolicCheckingType.CHECK_ASSERTIONS);
					symbolicCheckingHandler.checkAssertions(formulaItem);
					break;
				case CHECK_REFINEMENT:
					formula = noneCheckings.get(SymbolicCheckingType.CHECK_REFINEMENT);
					formulaItem = new SymbolicCheckingFormulaItem(formula, formula, SymbolicCheckingType.CHECK_REFINEMENT);
					symbolicCheckingHandler.checkRefinement(formulaItem);
					break;
				case IC3:
					formula = noneCheckings.get(SymbolicCheckingType.IC3);
					formulaItem = new SymbolicCheckingFormulaItem(formula, formula, SymbolicCheckingType.IC3);
					symbolicCheckingHandler.checkSymbolic(formulaItem, SymbolicModelcheckCommand.Algorithm.IC3);
					break;
				case TINDUCTION:
					formula = noneCheckings.get(SymbolicCheckingType.TINDUCTION);
					formulaItem = new SymbolicCheckingFormulaItem(formula, formula, SymbolicCheckingType.TINDUCTION);
					symbolicCheckingHandler.checkSymbolic(formulaItem, SymbolicModelcheckCommand.Algorithm.TINDUCTION);
					break;
				case KINDUCTION:
					formula = noneCheckings.get(SymbolicCheckingType.KINDUCTION);
					formulaItem = new SymbolicCheckingFormulaItem(formula, formula, SymbolicCheckingType.KINDUCTION);
					symbolicCheckingHandler.checkSymbolic(formulaItem, SymbolicModelcheckCommand.Algorithm.KINDUCTION);
					break;
				case BMC:
					formula = noneCheckings.get(SymbolicCheckingType.BMC);
					formulaItem = new SymbolicCheckingFormulaItem(formula, formula, SymbolicCheckingType.BMC);
					symbolicCheckingHandler.checkSymbolic(formulaItem, SymbolicModelcheckCommand.Algorithm.BMC);
					break;
				default:
					break;
			}
			injector.getInstance(SymbolicCheckingChoosingStage.class).close();
		};
	}
	
	@FXML
	public void initialize() {
		this.update();
		currentTrace.addListener((observable, from, to) -> update());
		btAdd.setOnAction(addHandler);
		btCheck.setOnAction(checkHandler);
	}
	
	public void changeFormula(SymbolicCheckingFormulaItem item) {
		btAdd.setText(bundle.getString("verifications.symbolic.input.change"));
		changeHandler.apply(item);
		btCheck.setText(bundle.getString("verifications.symbolic.input.changeAndCheck"));
		checkAndChangeHandler.apply(item);
		
		SymbolicCheckingChoosingStage choosingStage = injector.getInstance(SymbolicCheckingChoosingStage.class);
		choosingStage.select(item);
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
			formula = noneCheckings.get(choosingStage.getCheckingType());
		}
		SymbolicCheckingFormulaItem newItem = new SymbolicCheckingFormulaItem(formula, formula, choosingStage.getCheckingType());
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
	
	private void addFormula(boolean checking) {
		SymbolicCheckingType checkingType = injector.getInstance(SymbolicCheckingChoosingStage.class).getCheckingType();
		SymbolicCheckingFormulaItem formulaItem;
		String formula;
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
				formula = noneCheckings.get(SymbolicCheckingType.FIND_REDUNDANT_INVARIANTS);
				formulaItem = new SymbolicCheckingFormulaItem(formula, formula, SymbolicCheckingType.FIND_REDUNDANT_INVARIANTS);
				symbolicCheckingHandler.addFormula(formulaItem, checking);
				break;
			case FIND_DEADLOCK:
				formula = noneCheckings.get(SymbolicCheckingType.FIND_DEADLOCK);
				symbolicCheckingHandler.addFormula(formula, formula, SymbolicCheckingType.FIND_DEADLOCK, checking);
				break;
			case FIND_VALID_STATE:
				formulaItem = new SymbolicCheckingFormulaItem(tfFormula.getText(), tfFormula.getText(), 
						SymbolicCheckingType.FIND_VALID_STATE);
				symbolicCheckingHandler.addFormula(formulaItem, checking);
				break;
			case CHECK_ASSERTIONS:
				formula = noneCheckings.get(SymbolicCheckingType.CHECK_ASSERTIONS);
				formulaItem = new SymbolicCheckingFormulaItem(formula, formula, SymbolicCheckingType.CHECK_ASSERTIONS);
				symbolicCheckingHandler.addFormula(formulaItem, checking);
				break;
			case CHECK_REFINEMENT:
				formula = noneCheckings.get(SymbolicCheckingType.CHECK_REFINEMENT);
				formulaItem = new SymbolicCheckingFormulaItem(formula, formula, SymbolicCheckingType.CHECK_REFINEMENT);
				symbolicCheckingHandler.addFormula(formulaItem, checking);
				break;
			case IC3:
				formula = noneCheckings.get(SymbolicCheckingType.IC3);
				formulaItem = new SymbolicCheckingFormulaItem(formula, formula, SymbolicCheckingType.IC3);
				symbolicCheckingHandler.addFormula(formulaItem, checking);
				break;
			case TINDUCTION:
				formula = noneCheckings.get(SymbolicCheckingType.TINDUCTION);
				formulaItem = new SymbolicCheckingFormulaItem(formula, formula, SymbolicCheckingType.TINDUCTION);
				symbolicCheckingHandler.addFormula(formulaItem, checking);
				break;
			case KINDUCTION:
				formula = noneCheckings.get(SymbolicCheckingType.KINDUCTION);
				formulaItem = new SymbolicCheckingFormulaItem(formula, formula, SymbolicCheckingType.KINDUCTION);
				symbolicCheckingHandler.addFormula(formulaItem, checking);
				break;
			case BMC:
				formula = noneCheckings.get(SymbolicCheckingType.BMC);
				formulaItem = new SymbolicCheckingFormulaItem(formula, formula, SymbolicCheckingType.BMC);
				symbolicCheckingHandler.addFormula(formulaItem, checking);
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
	
	public void reset() {
		btAdd.setOnAction(addHandler);
		btCheck.setOnAction(checkHandler);
		btAdd.setText(bundle.getString("verifications.symbolic.add"));
		btCheck.setText(bundle.getString("verifications.symbolic.check"));
		tfFormula.clear();
		cbOperations.getSelectionModel().clearSelection();
	}

}
