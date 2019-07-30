package de.prob2.ui.symbolic;

import com.google.inject.Inject;
import com.google.inject.Injector;
import de.prob.statespace.LoadedMachine;
import de.prob2.ui.animation.symbolic.SymbolicAnimationFormulaItem;
import de.prob2.ui.animation.symbolic.testcasegeneration.MCDCInputView;
import de.prob2.ui.animation.symbolic.testcasegeneration.OperationCoverageInputView;
import de.prob2.ui.internal.PredicateBuilderView;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.AbstractResultHandler;
import de.prob2.ui.verifications.symbolicchecking.SymbolicCheckingFormulaItem;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public abstract class SymbolicFormulaInput<T extends SymbolicFormulaItem> extends VBox {
	
	protected final CurrentProject currentProject;
	
	@FXML
	protected Button btAdd;
	
	@FXML
	protected Button btCheck;
	
	@FXML
	protected TextField tfFormula;
	
	@FXML
	protected ChoiceBox<String> cbOperations;
	
	@FXML
	protected PredicateBuilderView predicateBuilderView;

	@FXML
	protected MCDCInputView mcdcInputView;

	@FXML
	protected OperationCoverageInputView operationCoverageInputView;

	protected final StageManager stageManager;

	protected final Injector injector;
	
	protected final ResourceBundle bundle;
	
	protected final CurrentTrace currentTrace;
	
	protected ArrayList<String> events;
	
	@Inject
	public SymbolicFormulaInput(final StageManager stageManager, final CurrentProject currentProject,
								final Injector injector, final ResourceBundle bundle,
								final CurrentTrace currentTrace) {
		this.stageManager = stageManager;
		this.currentProject = currentProject;
		this.currentTrace = currentTrace;
		this.events = new ArrayList<>();
		this.injector = injector;
		this.bundle = bundle;
	}

	@FXML
	public void initialize() {
		this.update();
		currentTrace.addListener((observable, from, to) -> update());
		setCheckListeners();
	}
	
	protected void update() {
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
		operationCoverageInputView.setTable(events);
	}
	
	protected abstract void setCheckListeners();
	
	public void changeGUIType(final SymbolicGUIType guiType) {
		this.getChildren().removeAll(tfFormula, cbOperations, predicateBuilderView, mcdcInputView, operationCoverageInputView);
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
			case MCDC:
				this.getChildren().add(0, mcdcInputView);
				break;
			case OPERATIONS:
				this.getChildren().add(0, operationCoverageInputView);
				break;
			default:
				throw new AssertionError("Unhandled GUI type: " + guiType);
		}
	}
	
	public void reset() {
		btAdd.setText(bundle.getString("common.buttons.add"));
		btCheck.setText(bundle.getString("symbolic.formulaInput.buttons.addAndCheck"));
		setCheckListeners();
		tfFormula.clear();
		predicateBuilderView.reset();
		mcdcInputView.reset();
		operationCoverageInputView.reset();
		cbOperations.getSelectionModel().clearSelection();
	}
	
	protected boolean updateFormula(T item, SymbolicView<T> view, SymbolicChoosingStage<T> choosingStage) {
		Machine currentMachine = currentProject.getCurrentMachine();
		String formula = null;
		Map<String, Object> additionalInformation = new HashMap<>();
		boolean valid = true;
		if(choosingStage.getGUIType() == SymbolicGUIType.TEXT_FIELD) {
			formula = tfFormula.getText();
		} else if(choosingStage.getGUIType() == SymbolicGUIType.CHOICE_BOX) {
			formula = cbOperations.getSelectionModel().getSelectedItem();
		} else if(choosingStage.getGUIType() == SymbolicGUIType.PREDICATE) {
			formula = predicateBuilderView.getPredicate();
		} else if(choosingStage.getGUIType() == SymbolicGUIType.MCDC) {
			String level = mcdcInputView.getLevel();
			String depth = mcdcInputView.getDepth();
			formula = "MCDC:" + level + "/" + "DEPTH:" + depth;
			valid = checkInteger(level) && checkInteger(depth);
			additionalInformation.put("maxDepth", depth);
			additionalInformation.put("level", level);
		} else if(choosingStage.getGUIType() == SymbolicGUIType.OPERATIONS) {
			List<String> operations = operationCoverageInputView.getOperations();
			String depth = operationCoverageInputView.getDepth();
			formula = "OPERATION:" + String.join(",", operations) + "/" + "DEPTH:" + depth;
			valid = !operations.isEmpty() && checkInteger(depth);
			additionalInformation.put("maxDepth", depth);
			additionalInformation.put("operations", operations);
		} else {
			formula = choosingStage.getExecutionType().getName();
		}
		SymbolicFormulaItem newItem;
		if(item.getClass() == SymbolicAnimationFormulaItem.class) {
			newItem = new SymbolicAnimationFormulaItem(formula, choosingStage.getExecutionType(), additionalInformation);
		} else {
			newItem = new SymbolicCheckingFormulaItem(formula, formula, choosingStage.getExecutionType());
		}
		if(choosingStage.getExecutionType() == SymbolicExecutionType.CHECK_ALL_OPERATIONS) {
			return true;
		}
		if(choosingStage.getExecutionType() == SymbolicExecutionType.INVARIANT && cbOperations.getSelectionModel().getSelectedItem() == null) {
			return true;
		}
		if(item.getClass() == SymbolicCheckingFormulaItem.class && !currentMachine.getSymbolicCheckingFormulas().contains(newItem)) {
			SymbolicExecutionType type = choosingStage.getExecutionType();
			item.setData(formula, type.getName(), formula, type);
			item.reset();
			view.refresh();
			return true;
		} else if(item.getClass() == SymbolicAnimationFormulaItem.class && !currentMachine.getSymbolicAnimationFormulas().contains(newItem)) {
			if(valid) {
				SymbolicExecutionType type = choosingStage.getExecutionType();
				((SymbolicAnimationFormulaItem) item).setData(formula, type.getName(), formula, type, additionalInformation);
				item.reset();
				view.refresh();
			}
			return true;
		}
		return false;
	}

	protected boolean checkInteger(String str) {
		try {
			Integer.parseInt(str);
		} catch(NumberFormatException e) {
			return false;
		}
		return true;
	}
	
	public void changeFormula(T item, SymbolicView<T> view, ISymbolicResultHandler resultHandler, SymbolicChoosingStage<T> stage) {
		btAdd.setText(bundle.getString("symbolic.formulaInput.buttons.change"));
		btCheck.setText(bundle.getString("symbolic.formulaInput.buttons.changeAndCheck"));
		setChangeListeners(item, view, resultHandler, stage);
		stage.select(item);
		if(stage.getGUIType() == SymbolicGUIType.TEXT_FIELD) {
			tfFormula.setText(item.getCode());
		} else if(stage.getGUIType() == SymbolicGUIType.PREDICATE) {
			predicateBuilderView.setFromPredicate(item.getName());
		} else if(stage.getGUIType() == SymbolicGUIType.CHOICE_BOX) {
			cbOperations.getItems().forEach(operationItem -> {
				if(operationItem.equals(item.getCode())) {
					cbOperations.getSelectionModel().select(operationItem);
					return;
				}
			});
		} else if(stage.getGUIType() == SymbolicGUIType.MCDC) {
			mcdcInputView.setItem((SymbolicAnimationFormulaItem) item);
		} else if(stage.getGUIType() == SymbolicGUIType.OPERATIONS) {
			operationCoverageInputView.setItem((SymbolicAnimationFormulaItem) item);
		}
		stage.show();
	}
	
	protected void setChangeListeners(T item, SymbolicView<T> view, ISymbolicResultHandler resultHandler, SymbolicChoosingStage<T> stage) {
		btAdd.setOnAction(e -> {
			if(updateFormula(item, view, stage)) {
				addFormula(false);
			} else {
				resultHandler.showAlreadyExists(AbstractResultHandler.ItemType.FORMULA);
			}
			stage.close();
		});
		
		btCheck.setOnAction(e-> {
			if(updateFormula(item, view, stage)) {
				checkFormula();
			} else {
				resultHandler.showAlreadyExists(AbstractResultHandler.ItemType.FORMULA);
			}
			stage.close();
		});
	}

	public abstract void checkFormula();

	protected abstract void addFormula(boolean checking);
	
}
