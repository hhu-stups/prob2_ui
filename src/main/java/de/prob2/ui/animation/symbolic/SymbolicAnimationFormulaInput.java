package de.prob2.ui.animation.symbolic;

import java.util.ArrayList;
import java.util.ResourceBundle;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.model.representation.AbstractElement;
import de.prob.model.representation.BEvent;
import de.prob2.ui.animation.symbolic.SymbolicAnimationItem.GUIType;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.AbstractResultHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;

@Singleton
public class SymbolicAnimationFormulaInput extends StackPane {
	
	
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
					symbolicAnimationFormulaHandler.handleDeadlock(tfFormula.getText(), false); 
					break;
				case SEQUENCE: 
					symbolicAnimationFormulaHandler.handleSequence(tfFormula.getText(), false); 
					break;
				case FIND_DEADLOCK: 
					symbolicAnimationFormulaHandler.findDeadlock(false); 
					break;
				case FIND_VALID_STATE:
					formulaItem = new SymbolicAnimationFormulaItem(tfFormula.getText(), tfFormula.getText(), 
							SymbolicAnimationType.FIND_VALID_STATE);
					symbolicAnimationFormulaHandler.findValidState(formulaItem, false);
					break;
				default:
					formulaItem = new SymbolicAnimationFormulaItem(animationType.name(), animationType.name(), animationType);
					switch(animationType) {
						case FIND_REDUNDANT_INVARIANTS: 
							symbolicAnimationFormulaHandler.findRedundantInvariants(formulaItem, false); 
							break;
				}
			}
			injector.getInstance(SymbolicAnimationChoosingStage.class).close();
		});
	}
	
	public void changeFormula(SymbolicAnimationFormulaItem item) {
		btAdd.setText(bundle.getString("verifications.symbolic.input.change"));
		btCheck.setText(bundle.getString("verifications.symbolic.input.changeAndCheck"));
		setChangeListeners(item);
		SymbolicAnimationChoosingStage choosingStage = injector.getInstance(SymbolicAnimationChoosingStage.class);
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
		} else {
			formula = choosingStage.getAnimationType().getName();
		}
		SymbolicAnimationFormulaItem newItem = new SymbolicAnimationFormulaItem(formula, formula, choosingStage.getAnimationType());
		if(!currentMachine.getSymbolicCheckingFormulas().contains(newItem)) {
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
	
	private void addFormula(boolean checking) {
		SymbolicAnimationType checkingType = injector.getInstance(SymbolicAnimationChoosingStage.class).getAnimationType();
		GUIType guiType = injector.getInstance(SymbolicAnimationChoosingStage.class).getGUIType();
		switch(guiType) {
			case TEXT_FIELD:
				symbolicAnimationFormulaHandler.addFormula(tfFormula.getText(), tfFormula.getText(), checkingType, checking);
				break;
			case NONE:
				symbolicAnimationFormulaHandler.addFormula(checkingType.name(), checkingType.name(), checkingType, checking);
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
