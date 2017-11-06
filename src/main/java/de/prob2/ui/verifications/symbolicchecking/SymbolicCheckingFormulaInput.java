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
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;

@Singleton
public class SymbolicCheckingFormulaInput extends StackPane {
	
	private final SymbolicCheckingFormulaHandler symbolicCheckingFormulaHandler;
	
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
	
	private final EnumMap<SymbolicCheckingType, String> noneCheckings;
	
	@Inject
	public SymbolicCheckingFormulaInput(final StageManager stageManager, 
										final SymbolicCheckingFormulaHandler symbolicCheckingFormulaHandler,
										final CurrentProject currentProject, final Injector injector, final ResourceBundle bundle,
										final CurrentTrace currentTrace) {
		this.symbolicCheckingFormulaHandler = symbolicCheckingFormulaHandler;
		this.currentProject = currentProject;
		this.currentTrace = currentTrace;
		this.events = new ArrayList<>();
		this.injector = injector;
		this.bundle = bundle;
		stageManager.loadFXML(this, "symbolic_checking_formula_input.fxml");
		noneCheckings = new EnumMap<SymbolicCheckingType, String>(SymbolicCheckingType.class);
		initializeNoneCheckings();
	}
	
	@FXML
	public void initialize() {
		this.update();
		currentTrace.addListener((observable, from, to) -> update());
		setCheckListeners();
	}
	
	private void initializeNoneCheckings() {
		noneCheckings.put(SymbolicCheckingType.FIND_DEADLOCK, FIND_DEADLOCK);
		noneCheckings.put(SymbolicCheckingType.FIND_REDUNDANT_INVARIANTS, FIND_REDUNDANT_INVARIANTS);
		noneCheckings.put(SymbolicCheckingType.CHECK_ASSERTIONS, ASSERTION_CHECKING);
		noneCheckings.put(SymbolicCheckingType.CHECK_REFINEMENT, REFINEMENT_CHECKING);
		noneCheckings.put(SymbolicCheckingType.IC3, IC3);
		noneCheckings.put(SymbolicCheckingType.TINDUCTION, TINDUCTION);
		noneCheckings.put(SymbolicCheckingType.KINDUCTION, KINDUCTION);
		noneCheckings.put(SymbolicCheckingType.BMC, BMC);
	}
	
	private void setChangeListeners(SymbolicCheckingFormulaItem item) {
		btAdd.setOnAction(e -> {
			if(!updateFormula(item)) {
				injector.getInstance(SymbolicCheckingResultHandler.class).showAlreadyExists(AbstractResultHandler.ItemType.FORMULA);
			}
			injector.getInstance(SymbolicCheckingChoosingStage.class).close();
		});
		
		btCheck.setOnAction(e-> {
			if(updateFormula(item)) {
				symbolicCheckingFormulaHandler.handleItem(item);
			} else {
				injector.getInstance(SymbolicCheckingResultHandler.class).showAlreadyExists(AbstractResultHandler.ItemType.FORMULA);
			}
			injector.getInstance(SymbolicCheckingChoosingStage.class).close();
		});
	}
	
	private void setCheckListeners() {
		btAdd.setOnAction(e -> addFormula(false));
		btCheck.setOnAction(e -> {
			SymbolicCheckingType checkingType = injector.getInstance(SymbolicCheckingChoosingStage.class).getCheckingType();
			SymbolicCheckingFormulaItem formulaItem = null;
			String formula;
			addFormula(true);
			switch(checkingType) {
				case INVARIANT: 
					symbolicCheckingFormulaHandler.handleInvariant(cbOperations.getSelectionModel().getSelectedItem());
					break;
				case DEADLOCK: 
					symbolicCheckingFormulaHandler.handleDeadlock(tfFormula.getText()); 
					break;
				case SEQUENCE: 
					symbolicCheckingFormulaHandler.handleSequence(tfFormula.getText()); 
					break;
				case CHECK_ALL_OPERATIONS:
					for(String event : events) {
						symbolicCheckingFormulaHandler.handleInvariant(event);
					}
					break;
				case FIND_DEADLOCK: 
					symbolicCheckingFormulaHandler.findDeadlock(); 
					break;
				case FIND_VALID_STATE:
					formulaItem = new SymbolicCheckingFormulaItem(tfFormula.getText(), tfFormula.getText(), 
							SymbolicCheckingType.FIND_VALID_STATE);
					symbolicCheckingFormulaHandler.findValidState(formulaItem);
					break;
				default:
					formula = noneCheckings.get(checkingType);
					formulaItem = new SymbolicCheckingFormulaItem(formula, formula, checkingType);
					switch(checkingType) {
					case FIND_REDUNDANT_INVARIANTS: 
						symbolicCheckingFormulaHandler.findRedundantInvariants(formulaItem); 
						break;
					case CHECK_ASSERTIONS: 
						symbolicCheckingFormulaHandler.handleAssertions(formulaItem); 
						break;
					case CHECK_REFINEMENT: 
						symbolicCheckingFormulaHandler.handleRefinement(formulaItem); 
						break;
					case IC3: 
						symbolicCheckingFormulaHandler.handleSymbolic(formulaItem, SymbolicModelcheckCommand.Algorithm.IC3); 
						break;
					case TINDUCTION: 
						symbolicCheckingFormulaHandler.handleSymbolic(formulaItem, SymbolicModelcheckCommand.Algorithm.TINDUCTION); 
						break;
					case KINDUCTION: 
						symbolicCheckingFormulaHandler.handleSymbolic(formulaItem, SymbolicModelcheckCommand.Algorithm.KINDUCTION);
						break;
					case BMC:
						symbolicCheckingFormulaHandler.handleSymbolic(formulaItem, SymbolicModelcheckCommand.Algorithm.BMC);
						break;
					default:
						break;
				}
			}
			injector.getInstance(SymbolicCheckingChoosingStage.class).close();
		});
	}
	
	public void changeFormula(SymbolicCheckingFormulaItem item) {
		btAdd.setText(bundle.getString("verifications.symbolic.input.change"));
		btCheck.setText(bundle.getString("verifications.symbolic.input.changeAndCheck"));
		setChangeListeners(item);
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
			item.setData(formula, type.name(), formula, type);
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
		GUIType guiType = injector.getInstance(SymbolicCheckingChoosingStage.class).getGUIType();
		String formula;
		switch(guiType) {
			case CHOICE_BOX:
				switch(checkingType) {
					case INVARIANT:
						String item = cbOperations.getSelectionModel().getSelectedItem();
						symbolicCheckingFormulaHandler.addFormula(item, item, SymbolicCheckingType.INVARIANT, checking);
						break;
					case CHECK_ALL_OPERATIONS:
						for(String event : events) {
							symbolicCheckingFormulaHandler.addFormula(event, event, SymbolicCheckingType.INVARIANT, checking);
						}
						break;
					default:
						break;
				}
			case TEXT_FIELD:
				symbolicCheckingFormulaHandler.addFormula(tfFormula.getText(), tfFormula.getText(), checkingType, checking);
				break;
			case NONE:
				formula = noneCheckings.get(checkingType);
				if(formula == null) {
					break;
				}
				symbolicCheckingFormulaHandler.addFormula(formula, formula, checkingType, checking);
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
		btAdd.setText(bundle.getString("verifications.symbolic.add"));
		btCheck.setText(bundle.getString("verifications.symbolic.check"));
		setCheckListeners();
		tfFormula.clear();
		cbOperations.getSelectionModel().clearSelection();
	}

}
