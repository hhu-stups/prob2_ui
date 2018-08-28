package de.prob2.ui.animation.symbolic;

import java.util.ResourceBundle;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

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
public class SymbolicAnimationFormulaInput extends SymbolicFormulaInput {
	
	
	private final SymbolicAnimationFormulaHandler symbolicAnimationFormulaHandler;
	
	@Inject
	public SymbolicAnimationFormulaInput(final StageManager stageManager, 
										final SymbolicAnimationFormulaHandler symbolicAnimationFormulaHandler,
										final CurrentProject currentProject, final Injector injector, final ResourceBundle bundle,
										final CurrentTrace currentTrace) {
		super(stageManager, currentProject, injector, bundle, currentTrace);
		this.symbolicAnimationFormulaHandler = symbolicAnimationFormulaHandler;
		stageManager.loadFXML(this, "symbolic_animation_formula_input.fxml");
	}
	
	private void setChangeListeners(SymbolicAnimationFormulaItem item) {
		btAdd.setOnAction(e -> {
			if(!updateFormula(item)) {
				injector.getInstance(SymbolicAnimationResultHandler.class).showAlreadyExists(AbstractResultHandler.ItemType.FORMULA);
			}
			injector.getInstance(SymbolicAnimationChoosingStage.class).close();
		});
		
		btCheck.setOnAction(e-> {
			if(updateFormula(item)) {
				symbolicAnimationFormulaHandler.handleItem(item, false);
			} else {
				injector.getInstance(SymbolicAnimationResultHandler.class).showAlreadyExists(AbstractResultHandler.ItemType.FORMULA);
			}
			injector.getInstance(SymbolicAnimationChoosingStage.class).close();
		});
	}
	
	protected void setCheckListeners() {
		btAdd.setOnAction(e -> addFormula(false));
		btCheck.setOnAction(e -> {
			SymbolicExecutionType animationType = injector.getInstance(SymbolicAnimationChoosingStage.class).getExecutionType();
			SymbolicAnimationFormulaItem formulaItem = null;
			addFormula(true);
			switch (animationType) {
			case DEADLOCK:
				symbolicAnimationFormulaHandler.handleDeadlock(predicateBuilderView.getPredicate(), false);
				break;
			case SEQUENCE:
				symbolicAnimationFormulaHandler.handleSequence(tfFormula.getText(), false);
				break;
			case FIND_DEADLOCK:
				symbolicAnimationFormulaHandler.findDeadlock(false);
				break;
			case FIND_VALID_STATE:
				formulaItem = new SymbolicAnimationFormulaItem(predicateBuilderView.getPredicate(),
						SymbolicExecutionType.FIND_VALID_STATE);
				symbolicAnimationFormulaHandler.findValidState(formulaItem, false);
				break;
			case FIND_REDUNDANT_INVARIANTS:
				formulaItem = new SymbolicAnimationFormulaItem(animationType.name(), animationType);
				symbolicAnimationFormulaHandler.findRedundantInvariants(formulaItem, false);
				break;
			default:
				break;
			}
			injector.getInstance(SymbolicAnimationChoosingStage.class).close();
		});
	}
	
	public void changeFormula(SymbolicAnimationFormulaItem item) {
		btAdd.setText(bundle.getString("verifications.symbolicchecking.formulaInput.buttons.change"));
		btCheck.setText(bundle.getString("verifications.symbolicchecking.formulaInput.buttons.changeAndCheck"));
		setChangeListeners(item);
		SymbolicAnimationChoosingStage choosingStage = injector.getInstance(SymbolicAnimationChoosingStage.class);
		choosingStage.select(item);
		if(choosingStage.getGUIType() == SymbolicGUIType.TEXT_FIELD) {
			tfFormula.setText(item.getCode());
		} else if(choosingStage.getGUIType() == SymbolicGUIType.PREDICATE) {
			predicateBuilderView.setItem(item);
		} else if(choosingStage.getGUIType() == SymbolicGUIType.CHOICE_BOX) {
			cbOperations.getItems().forEach(operationItem -> {
				if(operationItem.equals(item.getCode())) {
					cbOperations.getSelectionModel().select(operationItem);
					return;
				}
			});
		}
		injector.getInstance(SymbolicAnimationChoosingStage.class).showAndWait();
	}

	private boolean updateFormula(SymbolicAnimationFormulaItem item) {
		Machine currentMachine = currentProject.getCurrentMachine();
		String formula = null;
		SymbolicAnimationChoosingStage choosingStage = injector.getInstance(SymbolicAnimationChoosingStage.class);
		if(choosingStage.getGUIType() == SymbolicGUIType.TEXT_FIELD) {
			formula = tfFormula.getText();
		} else if(choosingStage.getGUIType() == SymbolicGUIType.CHOICE_BOX) {
			formula = cbOperations.getSelectionModel().getSelectedItem();
		} else if(choosingStage.getGUIType() == SymbolicGUIType.PREDICATE) {
			formula = predicateBuilderView.getPredicate();
		} else {
			formula = choosingStage.getExecutionType().getName();
		}
		SymbolicAnimationFormulaItem newItem = new SymbolicAnimationFormulaItem(formula, choosingStage.getExecutionType());
		if(!currentMachine.getSymbolicAnimationFormulas().contains(newItem)) {
			SymbolicExecutionType type = choosingStage.getExecutionType();
			item.setData(formula, type.getName(), formula, type);
			item.reset();
			injector.getInstance(SymbolicAnimationView.class).refresh();
			return true;
		}
		return false;
	}
	
	private void addFormula(boolean checking) {
		SymbolicExecutionType checkingType = injector.getInstance(SymbolicAnimationChoosingStage.class).getExecutionType();
		SymbolicGUIType guiType = injector.getInstance(SymbolicAnimationChoosingStage.class).getGUIType();
		switch(guiType) {
			case TEXT_FIELD:
				symbolicAnimationFormulaHandler.addFormula(tfFormula.getText(), checkingType, checking);
				break;
			case PREDICATE:
				final String predicate = predicateBuilderView.getPredicate();
				symbolicAnimationFormulaHandler.addFormula(predicate, checkingType, checking);
				break;
			case NONE:
				symbolicAnimationFormulaHandler.addFormula(checkingType.name(), checkingType, checking);
				break;
			default:
				break;
		}
		injector.getInstance(SymbolicAnimationChoosingStage.class).close();
	}
		
	@FXML
	public void cancel() {
		injector.getInstance(SymbolicAnimationChoosingStage.class).close();
	}

}
