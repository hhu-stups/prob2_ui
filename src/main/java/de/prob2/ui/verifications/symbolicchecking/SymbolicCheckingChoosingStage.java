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

import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

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
	private ChoiceBox<SymbolicCheckingType> cbChoice;
	
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
		cbChoice.setConverter(i18n.translateConverter());
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
	
	private void changeGUIType(final SymbolicCheckingType type) {
		formulaInput.getChildren().removeAll(cbOperations, predicateBuilderView, symbolicModelCheckAlgorithmChoiceBox);
		switch (type) {
			case CHECK_REFINEMENT:
			case CHECK_STATIC_ASSERTIONS:
			case CHECK_DYNAMIC_ASSERTIONS:
			case CHECK_WELL_DEFINEDNESS:
			case FIND_REDUNDANT_INVARIANTS:
				break;
			
			case SYMBOLIC_INVARIANT:
				formulaInput.getChildren().add(0, cbOperations);
				break;
			
			case SYMBOLIC_DEADLOCK:
				formulaInput.getChildren().add(0, predicateBuilderView);
				break;
			
			case SYMBOLIC_MODEL_CHECKING:
				formulaInput.getChildren().add(0, symbolicModelCheckAlgorithmChoiceBox);
				break;
			
			default:
				throw new AssertionError("Unhandled symbolic checking type: " + cbChoice.getValue());
		}
		this.sizeToScene();
	}
	
	private String extractFormula() {
		switch (cbChoice.getValue()) {
			case CHECK_REFINEMENT:
			case CHECK_STATIC_ASSERTIONS:
			case CHECK_DYNAMIC_ASSERTIONS:
			case CHECK_WELL_DEFINEDNESS:
			case FIND_REDUNDANT_INVARIANTS:
				return "";
			
			case SYMBOLIC_INVARIANT:
				if (this.checkAllOperations.equals(cbOperations.getSelectionModel().getSelectedItem())) {
					return "";
				} else {
					return cbOperations.getSelectionModel().getSelectedItem();
				}
			
			case SYMBOLIC_DEADLOCK:
				return predicateBuilderView.getPredicate();
			
			case SYMBOLIC_MODEL_CHECKING:
				return symbolicModelCheckAlgorithmChoiceBox.getSelectionModel().getSelectedItem().name();
			
			default:
				throw new AssertionError("Unhandled symbolic checking type: " + cbChoice.getValue());
		}
	}
	
	public void setData(SymbolicCheckingFormulaItem item) {
		cbChoice.getSelectionModel().select(item.getType());
		switch (item.getType()) {
			case CHECK_REFINEMENT:
			case CHECK_STATIC_ASSERTIONS:
			case CHECK_DYNAMIC_ASSERTIONS:
			case CHECK_WELL_DEFINEDNESS:
			case FIND_REDUNDANT_INVARIANTS:
				break;
			
			case SYMBOLIC_INVARIANT:
				if (item.getCode().isEmpty()) {
					cbOperations.getSelectionModel().select(this.checkAllOperations);
				} else {
					cbOperations.getSelectionModel().select(item.getCode());
				}
				break;
			
			case SYMBOLIC_DEADLOCK:
				predicateBuilderView.setFromPredicate(item.getCode());
				break;
			
			case SYMBOLIC_MODEL_CHECKING:
				symbolicModelCheckAlgorithmChoiceBox.getSelectionModel().select(SymbolicModelcheckCommand.Algorithm.valueOf(item.getCode()));
				break;
			
			default:
				throw new AssertionError("Unhandled symbolic checking type: " + cbChoice.getValue());
		}
		this.idTextField.setText(item.getId() == null ? "" : item.getId());
	}
	
	@FXML
	private void ok() {
		final String id = idTextField.getText().trim().isEmpty() ? null : idTextField.getText();
		this.result = new SymbolicCheckingFormulaItem(id, this.extractFormula(), cbChoice.getValue());
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
