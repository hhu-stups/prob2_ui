package de.prob2.ui.animation.symbolic;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ResourceBundle;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.statespace.LoadedMachine;
import de.prob2.ui.animation.symbolic.SymbolicAnimationItem.GUIType;
import de.prob2.ui.internal.PredicateBuilderView;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.AbstractResultHandler;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

@Singleton
public class SymbolicAnimationFormulaInput extends VBox {
	private final SymbolicAnimationFormulaHandler symbolicAnimationFormulaHandler;
	
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
	public SymbolicAnimationFormulaInput(final StageManager stageManager, 
										final SymbolicAnimationFormulaHandler symbolicAnimationFormulaHandler,
										final CurrentProject currentProject, final Injector injector, final ResourceBundle bundle,
										final CurrentTrace currentTrace) {
		this.symbolicAnimationFormulaHandler = symbolicAnimationFormulaHandler;
		this.currentProject = currentProject;
		this.currentTrace = currentTrace;
		this.events = new ArrayList<>();
		this.injector = injector;
		this.bundle = bundle;
		stageManager.loadFXML(this, "symbolic_animation_formula_input.fxml");
	}
	
	@FXML
	public void initialize() {
		this.update();
		currentTrace.addListener((observable, from, to) -> update());
		setCheckListeners();
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
	
	private void setCheckListeners() {
		btAdd.setOnAction(e -> addFormula(false));
		btCheck.setOnAction(e -> {
			SymbolicAnimationType animationType = injector.getInstance(SymbolicAnimationChoosingStage.class).getAnimationType();
			SymbolicAnimationFormulaItem formulaItem = null;
			addFormula(true);
			switch(animationType) {
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
					formulaItem = new SymbolicAnimationFormulaItem(predicateBuilderView.getPredicate(), SymbolicAnimationType.FIND_VALID_STATE);
					symbolicAnimationFormulaHandler.findValidState(formulaItem, false);
					break;
				default:
					formulaItem = new SymbolicAnimationFormulaItem(animationType.name(), animationType);
					switch(animationType) {
						case FIND_REDUNDANT_INVARIANTS: 
							symbolicAnimationFormulaHandler.findRedundantInvariants(formulaItem, false); 
							break;
					default:
						break;
				}
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
		if(choosingStage.getGUIType() == GUIType.TEXT_FIELD) {
			tfFormula.setText(item.getCode());
		} else if(choosingStage.getGUIType() == GUIType.PREDICATE) {
			predicateBuilderView.setItem(item);
		} else if(choosingStage.getGUIType() == GUIType.CHOICE_BOX) {
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
		if(choosingStage.getGUIType() == GUIType.TEXT_FIELD) {
			formula = tfFormula.getText();
		} else if(choosingStage.getGUIType() == GUIType.CHOICE_BOX) {
			formula = cbOperations.getSelectionModel().getSelectedItem();
		} else if(choosingStage.getGUIType() == GUIType.PREDICATE) {
			formula = predicateBuilderView.getPredicate();
		} else {
			formula = choosingStage.getAnimationType().getName();
		}
		SymbolicAnimationFormulaItem newItem = new SymbolicAnimationFormulaItem(formula, choosingStage.getAnimationType());
		if(!currentMachine.getSymbolicAnimationFormulas().contains(newItem)) {
			SymbolicAnimationType type = choosingStage.getAnimationType();
			item.setData(formula, type.getName(), formula, type);
			item.reset();
			injector.getInstance(SymbolicAnimationView.class).refresh();
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
		SymbolicAnimationType checkingType = injector.getInstance(SymbolicAnimationChoosingStage.class).getAnimationType();
		GUIType guiType = injector.getInstance(SymbolicAnimationChoosingStage.class).getGUIType();
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
	
	public void reset() {
		btAdd.setText(bundle.getString("common.buttons.add"));
		btCheck.setText(bundle.getString("verifications.symbolicchecking.formulaInput.buttons.addAndCheck"));
		setCheckListeners();
		tfFormula.clear();
		cbOperations.getSelectionModel().clearSelection();
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
}
