package de.prob2.ui.verifications.symbolicchecking;

import java.util.ArrayList;
import java.util.List;

import com.google.inject.Inject;

import de.prob.animator.command.SymbolicModelcheckCommand;
import de.prob.statespace.LoadedMachine;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.sharedviews.PredicateBuilderTableItem;
import de.prob2.ui.sharedviews.PredicateBuilderView;
import de.prob2.ui.verifications.WellDefinednessCheckingItem;
import de.prob2.ui.verifications.cbc.CBCDeadlockFreedomCheckingItem;
import de.prob2.ui.verifications.cbc.CBCDynamicAssertionCheckingItem;
import de.prob2.ui.verifications.cbc.CBCFindRedundantInvariantsItem;
import de.prob2.ui.verifications.cbc.CBCInvariantPreservationCheckingItem;
import de.prob2.ui.verifications.cbc.CBCRefinementCheckingItem;
import de.prob2.ui.verifications.cbc.CBCStaticAssertionCheckingItem;
import de.prob2.ui.verifications.type.BuiltinValidationTaskTypes;
import de.prob2.ui.verifications.type.ValidationTaskType;

import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;

public final class SymbolicCheckingChoosingStage extends Stage {
	@FXML
	private ChoiceBox<String> cbOperations;
	
	@FXML
	private PredicateBuilderView predicateBuilderView;
	
	@FXML
	private ChoiceBox<SymbolicModelcheckCommand.Algorithm> symbolicModelCheckAlgorithmChoiceBox;
	
	@FXML
	private VBox formulaInput;
	
	@FXML
	private ChoiceBox<ValidationTaskType<?>> cbChoice;
	
	@FXML
	private TextField idTextField;
	
	private final I18n i18n;
	
	private final String checkAllOperations;
	
	private SymbolicCheckingFormulaItem result;
	
	@Inject
	private SymbolicCheckingChoosingStage(final StageManager stageManager, final I18n i18n) {
		this.i18n = i18n;
		this.checkAllOperations = i18n.translate("verifications.symbolicchecking.choice.checkAllOperations");
		
		this.initModality(Modality.APPLICATION_MODAL);
		stageManager.loadFXML(this, "symbolic_checking_choice.fxml");
	}
	
	@FXML
	private void initialize() {
		formulaInput.visibleProperty().bind(cbChoice.getSelectionModel().selectedItemProperty().isNotNull());
		cbChoice.getSelectionModel().selectedItemProperty().addListener((o, from, to) -> {
			if(to == null) {
				return;
			}
			changeGUIType(to);
			this.sizeToScene();
		});
		cbChoice.setConverter(new StringConverter<>() {
			@Override
			public String toString(ValidationTaskType<?> object) {
				if (object == null) {
					return "";
				} else if (object == BuiltinValidationTaskTypes.CBC_INVARIANT_PRESERVATION_CHECKING) {
					return i18n.translate("verifications.symbolicchecking.type.invariant");
				} else if (object == BuiltinValidationTaskTypes.CBC_DEADLOCK_FREEDOM_CHECKING) {
					return i18n.translate("verifications.symbolicchecking.type.deadlock");
				} else if (object == BuiltinValidationTaskTypes.CBC_REFINEMENT_CHECKING) {
					return i18n.translate("verifications.symbolicchecking.type.refinementChecking");
				} else if (object == BuiltinValidationTaskTypes.CBC_STATIC_ASSERTION_CHECKING) {
					return i18n.translate("verifications.symbolicchecking.type.staticAssertionChecking");
				} else if (object == BuiltinValidationTaskTypes.CBC_DYNAMIC_ASSERTION_CHECKING) {
					return i18n.translate("verifications.symbolicchecking.type.dynamicAssertionChecking");
				} else if (object == BuiltinValidationTaskTypes.WELL_DEFINEDNESS_CHECKING) {
					return i18n.translate("verifications.symbolicchecking.type.wellDefinednessChecking");
				} else if (object == BuiltinValidationTaskTypes.CBC_FIND_REDUNDANT_INVARIANTS) {
					return i18n.translate("verifications.symbolicchecking.type.findRedundantInvariants");
				} else if (object == BuiltinValidationTaskTypes.SYMBOLIC_MODEL_CHECKING) {
					return i18n.translate("verifications.symbolicchecking.type.symbolicModelChecking");
				} else {
					return object.getKey();
				}
			}
			
			@Override
			public ValidationTaskType<?> fromString(String string) {
				throw new UnsupportedOperationException("Conversion from String to ValidationTaskType not supported");
			}
		});
		symbolicModelCheckAlgorithmChoiceBox.getItems().setAll(SymbolicModelcheckCommand.Algorithm.values());
		symbolicModelCheckAlgorithmChoiceBox.getSelectionModel().select(0);
		this.setResizable(true);
	}
	
	public void setMachine(final LoadedMachine loadedMachine) {
		cbOperations.getItems().setAll(this.checkAllOperations);
		cbOperations.getSelectionModel().select(this.checkAllOperations);
		final List<PredicateBuilderTableItem> items = new ArrayList<>();
		if (loadedMachine != null) {
			cbOperations.getItems().addAll(loadedMachine.getOperationNames());
			loadedMachine.getConstantNames().forEach(s -> items.add(new PredicateBuilderTableItem(s, "", PredicateBuilderTableItem.VariableType.CONSTANT)));
			loadedMachine.getVariableNames().forEach(s -> items.add(new PredicateBuilderTableItem(s, "", PredicateBuilderTableItem.VariableType.VARIABLE)));
		}
		predicateBuilderView.setItems(items);
	}
	
	private void changeGUIType(ValidationTaskType<?> type) {
		formulaInput.getChildren().removeAll(cbOperations, predicateBuilderView, symbolicModelCheckAlgorithmChoiceBox);
		if (BuiltinValidationTaskTypes.CBC_INVARIANT_PRESERVATION_CHECKING.equals(type)) {
			formulaInput.getChildren().add(0, cbOperations);
		} else if (BuiltinValidationTaskTypes.CBC_DEADLOCK_FREEDOM_CHECKING.equals(type)) {
			formulaInput.getChildren().add(0, predicateBuilderView);
		} else if (BuiltinValidationTaskTypes.SYMBOLIC_MODEL_CHECKING.equals(type)) {
			formulaInput.getChildren().add(0, symbolicModelCheckAlgorithmChoiceBox);
		}
		this.sizeToScene();
	}
	
	public void setData(SymbolicCheckingFormulaItem item) {
		cbChoice.getSelectionModel().select(item.getTaskType());
		if (item instanceof CBCInvariantPreservationCheckingItem invariantItem) {
			if (invariantItem.getOperationName() == null) {
				cbOperations.getSelectionModel().select(this.checkAllOperations);
			} else {
				cbOperations.getSelectionModel().select(invariantItem.getOperationName());
			}
		} else if (item instanceof CBCDeadlockFreedomCheckingItem deadlockItem) {
			predicateBuilderView.setFromPredicate(deadlockItem.getPredicate());
		} else if (item instanceof SymbolicModelCheckingItem symbolicItem) {
			symbolicModelCheckAlgorithmChoiceBox.getSelectionModel().select(symbolicItem.getAlgorithm());
		}
		this.idTextField.setText(item.getId() == null ? "" : item.getId());
	}
	
	@FXML
	private void ok() {
		final String id = idTextField.getText().trim().isEmpty() ? null : idTextField.getText();
		ValidationTaskType<?> type = cbChoice.getValue();
		if (BuiltinValidationTaskTypes.CBC_INVARIANT_PRESERVATION_CHECKING.equals(type)) {
			String operationName;
			if (this.checkAllOperations.equals(cbOperations.getSelectionModel().getSelectedItem())) {
				operationName = null;
			} else {
				operationName = cbOperations.getSelectionModel().getSelectedItem();
			}
			this.result = new CBCInvariantPreservationCheckingItem(id, operationName);
		} else if (BuiltinValidationTaskTypes.CBC_DEADLOCK_FREEDOM_CHECKING.equals(type)) {
			this.result = new CBCDeadlockFreedomCheckingItem(id, predicateBuilderView.getPredicate());
		} else if (BuiltinValidationTaskTypes.CBC_REFINEMENT_CHECKING.equals(type)) {
			this.result = new CBCRefinementCheckingItem(id);
		} else if (BuiltinValidationTaskTypes.CBC_STATIC_ASSERTION_CHECKING.equals(type)) {
			this.result = new CBCStaticAssertionCheckingItem(id);
		} else if (BuiltinValidationTaskTypes.CBC_DYNAMIC_ASSERTION_CHECKING.equals(type)) {
			this.result = new CBCDynamicAssertionCheckingItem(id);
		} else if (BuiltinValidationTaskTypes.WELL_DEFINEDNESS_CHECKING.equals(type)) {
			this.result = new WellDefinednessCheckingItem(id);
		} else if (BuiltinValidationTaskTypes.CBC_FIND_REDUNDANT_INVARIANTS.equals(type)) {
			this.result = new CBCFindRedundantInvariantsItem(id);
		} else if (BuiltinValidationTaskTypes.SYMBOLIC_MODEL_CHECKING.equals(type)) {
			this.result = new SymbolicModelCheckingItem(id, symbolicModelCheckAlgorithmChoiceBox.getValue());
		} else {
			throw new AssertionError("Unhandled symbolic checking type: " + type);
		}
		this.close();
	}
	
	@FXML
	public void cancel() {
		this.result = null;
		this.close();
	}
	
	public SymbolicCheckingFormulaItem getResult() {
		return result;
	}
}
