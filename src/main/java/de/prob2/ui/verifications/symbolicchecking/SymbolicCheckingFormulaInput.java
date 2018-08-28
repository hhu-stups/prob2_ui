package de.prob2.ui.verifications.symbolicchecking;


import java.util.ResourceBundle;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.animator.command.SymbolicModelcheckCommand;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.symbolic.SymbolicExecutionType;
import de.prob2.ui.symbolic.SymbolicFormulaInput;
import de.prob2.ui.symbolic.SymbolicGUIType;
import de.prob2.ui.verifications.AbstractResultHandler;

import javafx.fxml.FXML;


@Singleton
public class SymbolicCheckingFormulaInput extends SymbolicFormulaInput {
	
	private final SymbolicCheckingFormulaHandler symbolicCheckingFormulaHandler;
	
	@Inject
	public SymbolicCheckingFormulaInput(final StageManager stageManager, 
										final SymbolicCheckingFormulaHandler symbolicCheckingFormulaHandler,
										final CurrentProject currentProject, final Injector injector, final ResourceBundle bundle,
										final CurrentTrace currentTrace) {
		super(stageManager, currentProject, injector, bundle, currentTrace);
		this.symbolicCheckingFormulaHandler = symbolicCheckingFormulaHandler;
		stageManager.loadFXML(this, "symbolic_checking_formula_input.fxml");
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
	
	protected void setCheckListeners() {
		btAdd.setOnAction(e -> addFormula(false));
		btCheck.setOnAction(e -> {
			SymbolicExecutionType checkingType = injector.getInstance(SymbolicCheckingChoosingStage.class).getCheckingType();
			SymbolicCheckingFormulaItem formulaItem = null;
			addFormula(true);
			switch(checkingType) {
				case INVARIANT: 
					symbolicCheckingFormulaHandler.handleInvariant(cbOperations.getSelectionModel().getSelectedItem(), false);
					break;
				case CHECK_ALL_OPERATIONS:
					events.forEach(event -> symbolicCheckingFormulaHandler.handleInvariant(event, true));
					break;
				default:
					formulaItem = new SymbolicCheckingFormulaItem(checkingType.name(), checkingType.name(), checkingType);
					switch(checkingType) {
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
		if(choosingStage.getGUIType() == SymbolicGUIType.TEXT_FIELD) {
			tfFormula.setText(item.getCode());
		} else if(choosingStage.getGUIType() == SymbolicGUIType.CHOICE_BOX) {
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
		if(choosingStage.getGUIType() == SymbolicGUIType.TEXT_FIELD) {
			formula = tfFormula.getText();
		} else if(choosingStage.getGUIType() == SymbolicGUIType.CHOICE_BOX) {
			formula = cbOperations.getSelectionModel().getSelectedItem();
		} else {
			formula = choosingStage.getCheckingType().getName();
		}
		SymbolicCheckingFormulaItem newItem = new SymbolicCheckingFormulaItem(formula, formula, choosingStage.getCheckingType());
		if(!currentMachine.getSymbolicCheckingFormulas().contains(newItem)) {
			SymbolicExecutionType type = choosingStage.getCheckingType();
			item.setData(formula, type.getName(), formula, type);
			item.reset();
			injector.getInstance(SymbolicCheckingView.class).refresh();
			return true;
		}
		return false;
	}
	
	private void addFormula(boolean checking) {
		SymbolicExecutionType checkingType = injector.getInstance(SymbolicCheckingChoosingStage.class).getCheckingType();
		if(checkingType == SymbolicExecutionType.INVARIANT && cbOperations.getSelectionModel().getSelectedItem() == null) {
			injector.getInstance(SymbolicCheckingChoosingStage.class).close();
			return;
		}
		SymbolicGUIType guiType = injector.getInstance(SymbolicCheckingChoosingStage.class).getGUIType();
		switch(guiType) {
			case CHOICE_BOX:
				switch(checkingType) {
					case INVARIANT:
						String item = cbOperations.getSelectionModel().getSelectedItem();
						symbolicCheckingFormulaHandler.addFormula(item, item, SymbolicExecutionType.INVARIANT, checking);
						break;
					case CHECK_ALL_OPERATIONS:
						for(String event : events) {
							symbolicCheckingFormulaHandler.addFormula(event, event, SymbolicExecutionType.INVARIANT, checking);
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
				if(checkingType == SymbolicExecutionType.CHECK_ALL_OPERATIONS) {
					for(String event : events) {
						symbolicCheckingFormulaHandler.addFormula(event, event, SymbolicExecutionType.INVARIANT, checking);
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

}
