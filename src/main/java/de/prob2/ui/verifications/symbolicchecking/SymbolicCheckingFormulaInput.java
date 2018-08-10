package de.prob2.ui.verifications.symbolicchecking;


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ResourceBundle;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.animator.command.SymbolicModelcheckCommand;
import de.prob.statespace.LoadedMachine;
import de.prob2.ui.internal.PredicateBuilderView;
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
import javafx.scene.layout.VBox;

@Singleton
public class SymbolicCheckingFormulaInput extends VBox {
	
	private final SymbolicCheckingFormulaHandler symbolicCheckingFormulaHandler;
	
	private final CurrentProject currentProject;
	
	@FXML
	private Button btAdd;
	
	@FXML
	private Button btCheck;
	
	@FXML
	private TextField tfFormula;
	
	@FXML
	private ChoiceBox<String> cbOperations;
	
	@FXML
	private PredicateBuilderView predicateBuilderView;
	
	private final Injector injector;
	
	private final ResourceBundle bundle;
	
	private final CurrentTrace currentTrace;
	
	private ArrayList<String> events;
	
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
	}
	
	@FXML
	public void initialize() {
		this.update();
		currentTrace.addListener((observable, from, to) -> update());
		setCheckListeners();
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
				symbolicCheckingFormulaHandler.handleItem(item, false);
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
			addFormula(true);
			switch(checkingType) {
				case INVARIANT: 
					symbolicCheckingFormulaHandler.handleInvariant(cbOperations.getSelectionModel().getSelectedItem(), false);
					break;
				case DEADLOCK: 
					symbolicCheckingFormulaHandler.handleDeadlock(tfFormula.getText(), false); 
					break;
				case SEQUENCE: 
					symbolicCheckingFormulaHandler.handleSequence(tfFormula.getText(), false); 
					break;
				case CHECK_ALL_OPERATIONS:
					events.forEach(event -> symbolicCheckingFormulaHandler.handleInvariant(event, true));
					break;
				case FIND_DEADLOCK: 
					symbolicCheckingFormulaHandler.findDeadlock(false); 
					break;
				case FIND_VALID_STATE:
					formulaItem = new SymbolicCheckingFormulaItem(tfFormula.getText(), tfFormula.getText(), 
							SymbolicCheckingType.FIND_VALID_STATE);
					symbolicCheckingFormulaHandler.findValidState(formulaItem, false);
					break;
				default:
					formulaItem = new SymbolicCheckingFormulaItem(checkingType.name(), checkingType.name(), checkingType);
					switch(checkingType) {
						case FIND_REDUNDANT_INVARIANTS: 
							symbolicCheckingFormulaHandler.findRedundantInvariants(formulaItem, false); 
							break;
						case CHECK_ASSERTIONS: 
							symbolicCheckingFormulaHandler.handleAssertions(formulaItem, false);
							break;
						case CHECK_REFINEMENT: 
							symbolicCheckingFormulaHandler.handleRefinement(formulaItem, false); 
							break;
						default:
							SymbolicModelcheckCommand.Algorithm algorithm = checkingType.getAlgorithm();
							if(algorithm != null) {
								symbolicCheckingFormulaHandler.handleSymbolic(formulaItem, algorithm, false);
							}
							break;
				}
			}
			injector.getInstance(SymbolicCheckingChoosingStage.class).close();
		});
	}
	
	public void changeFormula(SymbolicCheckingFormulaItem item) {
		btAdd.setText(bundle.getString("verifications.symbolicchecking.formulaInput.buttons.change"));
		btCheck.setText(bundle.getString("verifications.symbolicchecking.formulaInput.buttons.changeAndCheck"));
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
			formula = choosingStage.getCheckingType().getName();
		}
		SymbolicCheckingFormulaItem newItem = new SymbolicCheckingFormulaItem(formula, formula, choosingStage.getCheckingType());
		if(!currentMachine.getSymbolicCheckingFormulas().contains(newItem)) {
			SymbolicCheckingType type = choosingStage.getCheckingType();
			item.setData(formula, type.getName(), formula, type);
			item.reset();
			injector.getInstance(SymbolicCheckingView.class).refresh();
			return true;
		}
		return false;
	}
	
	private void update() {
		events.clear();
		final Map<String, String> items = new LinkedHashMap<>();
		if (currentTrace.get() != null) {
			final LoadedMachine loadedMachine = currentTrace.getStateSpace().getLoadedMachine();
			if (loadedMachine != null) {
				events.addAll(loadedMachine.getOperationNames());
				loadedMachine.getConstantNames().forEach(s -> items.put(s, ""));
				loadedMachine.getVariableNames().forEach(s -> items.put(s, ""));
			}
		}
		cbOperations.getItems().setAll(events);
		predicateBuilderView.setItems(items);
	}
	
	private void addFormula(boolean checking) {
		SymbolicCheckingType checkingType = injector.getInstance(SymbolicCheckingChoosingStage.class).getCheckingType();
		if(checkingType == SymbolicCheckingType.INVARIANT && cbOperations.getSelectionModel().getSelectedItem() == null) {
			injector.getInstance(SymbolicCheckingChoosingStage.class).close();
			return;
		}
		GUIType guiType = injector.getInstance(SymbolicCheckingChoosingStage.class).getGUIType();
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
						throw new AssertionError("Unhandled checking type: " + checkingType);
				}
				break;
			case TEXT_FIELD:
				symbolicCheckingFormulaHandler.addFormula(tfFormula.getText(), tfFormula.getText(), checkingType, checking);
				break;
			case PREDICATE:
				final String predicate = predicateBuilderView.getPredicate();
				symbolicCheckingFormulaHandler.addFormula(predicate, predicate, checkingType, checking);
				break;
			case NONE:
				if(checkingType == SymbolicCheckingType.CHECK_ALL_OPERATIONS) {
					for(String event : events) {
						symbolicCheckingFormulaHandler.addFormula(event, event, SymbolicCheckingType.INVARIANT, checking);
					}
				} else {
					symbolicCheckingFormulaHandler.addFormula(checkingType.name(), checkingType.name(), checkingType, checking);
				}
				break;
			default:
				throw new AssertionError("Unhandled GUI type: " + guiType);
		}
		injector.getInstance(SymbolicCheckingChoosingStage.class).close();
	}
		
	@FXML
	public void cancel() {
		injector.getInstance(SymbolicCheckingChoosingStage.class).close();
	}
	
	public void changeGUIType(final GUIType guiType) {
		this.getChildren().removeAll(tfFormula, cbOperations, predicateBuilderView);
		switch (guiType) {
			case NONE:
				break;
			
			case TEXT_FIELD:
				this.getChildren().add(0, tfFormula);
				break;
			
			case CHOICE_BOX:
				this.getChildren().add(0, cbOperations);
				break;
			
			case PREDICATE:
				this.getChildren().add(0, predicateBuilderView);
				break;
			
			default:
				throw new AssertionError("Unhandled GUI type: " + guiType);
		}
	}
	
	public void reset() {
		btAdd.setText(bundle.getString("common.buttons.add"));
		btCheck.setText(bundle.getString("verifications.symbolicchecking.formulaInput.buttons.addAndCheck"));
		setCheckListeners();
		tfFormula.clear();
		cbOperations.getSelectionModel().clearSelection();
	}

}
