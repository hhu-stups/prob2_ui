package de.prob2.ui.symbolic;

import com.google.inject.Inject;
import com.google.inject.Injector;
import de.prob.statespace.LoadedMachine;
import de.prob2.ui.animation.symbolic.SymbolicAnimationFormulaItem;
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
import java.util.LinkedHashMap;
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
	
	protected final Injector injector;
	
	protected final ResourceBundle bundle;
	
	protected final CurrentTrace currentTrace;
	
	protected ArrayList<String> events;
	
	@Inject
	public SymbolicFormulaInput(final StageManager stageManager, final CurrentProject currentProject, 
								final Injector injector, final ResourceBundle bundle,
								final CurrentTrace currentTrace) {
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
	}
	
	protected abstract void setCheckListeners();
	
	public void changeGUIType(final SymbolicGUIType guiType) {
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
		btCheck.setText(bundle.getString("symbolic.formulaInput.buttons.addAndCheck"));
		setCheckListeners();
		tfFormula.clear();
		predicateBuilderView.reset();
		cbOperations.getSelectionModel().clearSelection();
	}
	
	protected boolean updateFormula(T item, SymbolicView<T> view, SymbolicChoosingStage<T> choosingStage) {
		Machine currentMachine = currentProject.getCurrentMachine();
		String formula = null;
		if(choosingStage.getGUIType() == SymbolicGUIType.TEXT_FIELD) {
			formula = tfFormula.getText();
		} else if(choosingStage.getGUIType() == SymbolicGUIType.CHOICE_BOX) {
			formula = cbOperations.getSelectionModel().getSelectedItem();
		} else if(choosingStage.getGUIType() == SymbolicGUIType.PREDICATE) {
			formula = predicateBuilderView.getPredicate();
		} else {
			formula = choosingStage.getExecutionType().getName();
		}
		SymbolicFormulaItem newItem;
		if(item.getClass() == SymbolicAnimationFormulaItem.class) {
			newItem = new SymbolicAnimationFormulaItem(formula, choosingStage.getExecutionType());
		} else {
			newItem = new SymbolicCheckingFormulaItem(formula, formula, choosingStage.getExecutionType());
		}
		if(choosingStage.getExecutionType() == SymbolicExecutionType.CHECK_ALL_OPERATIONS) {
			return true;
		}
		if(choosingStage.getExecutionType() == SymbolicExecutionType.INVARIANT && cbOperations.getSelectionModel().getSelectedItem() == null) {
			return true;
		}
		if(!currentMachine.getSymbolicCheckingFormulas().contains(newItem)) {
			SymbolicExecutionType type = choosingStage.getExecutionType();
			item.setData(formula, type.getName(), formula, type);
			item.reset();
			view.refresh();
			return true;
		}
		return false;
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
