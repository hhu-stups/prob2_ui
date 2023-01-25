package de.prob2.ui.symbolic;

import java.util.ArrayList;
import java.util.List;

import de.prob.animator.command.SymbolicModelcheckCommand;
import de.prob.statespace.LoadedMachine;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.sharedviews.PredicateBuilderTableItem;
import de.prob2.ui.sharedviews.PredicateBuilderView;

import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public abstract class SymbolicChoosingStage<T extends SymbolicItem<ET>, ET extends SymbolicExecutionType> extends Stage {
	@FXML
	private TextField tfFormula;
	
	@FXML
	private ChoiceBox<String> cbOperations;
	
	@FXML
	private PredicateBuilderView predicateBuilderView;
	
	@FXML
	private ChoiceBox<SymbolicModelcheckCommand.Algorithm> symbolicModelCheckAlgorithmChoiceBox;
	
	@FXML
	private VBox formulaInput;
	
	@FXML
	private ChoiceBox<ET> cbChoice;
	
	private final I18n i18n;
	
	private final CurrentTrace currentTrace;
	
	private final String checkAllOperations;

	private T result;
	
	protected SymbolicChoosingStage(final I18n i18n, final CurrentTrace currentTrace) {
		this.i18n = i18n;
		this.currentTrace = currentTrace;
		this.checkAllOperations = i18n.translate("verifications.symbolicchecking.choice.checkAllOperations");
		
		this.initModality(Modality.APPLICATION_MODAL);
	}
	
	@FXML
	public void initialize() {
		this.update();
		currentTrace.addListener((observable, from, to) -> update());
		formulaInput.visibleProperty().bind(cbChoice.getSelectionModel().selectedItemProperty().isNotNull());
		cbChoice.getSelectionModel().selectedItemProperty().addListener((o, from, to) -> {
			if(to == null) {
				return;
			}
			changeGUIType(getGUIType(to));
			this.sizeToScene();
		});
		cbChoice.setConverter(i18n.translateConverter());
		symbolicModelCheckAlgorithmChoiceBox.getItems().setAll(SymbolicModelcheckCommand.Algorithm.values());
		symbolicModelCheckAlgorithmChoiceBox.getSelectionModel().select(0);
		this.setResizable(true);
	}
	
	public abstract SymbolicGUIType getGUIType(final ET item);
	
	public SymbolicGUIType getGUIType() {
		return getGUIType(cbChoice.getSelectionModel().getSelectedItem());
	}
	
	public ET getExecutionType() {
		return cbChoice.getSelectionModel().getSelectedItem();
	}
	
	protected void update() {
		cbOperations.getItems().setAll(this.checkAllOperations);
		cbOperations.getSelectionModel().select(this.checkAllOperations);
		final List<PredicateBuilderTableItem> items = new ArrayList<>();
		if (currentTrace.get() != null) {
			final LoadedMachine loadedMachine = currentTrace.getStateSpace().getLoadedMachine();
			if (loadedMachine != null) {
				cbOperations.getItems().addAll(loadedMachine.getOperationNames());
				loadedMachine.getConstantNames().forEach(s -> items.add(new PredicateBuilderTableItem(s, "", PredicateBuilderTableItem.VariableType.CONSTANT)));
				loadedMachine.getVariableNames().forEach(s -> items.add(new PredicateBuilderTableItem(s, "", PredicateBuilderTableItem.VariableType.VARIABLE)));
			}
		}
		predicateBuilderView.setItems(items);
	}
	
	public void changeGUIType(final SymbolicGUIType guiType) {
		formulaInput.getChildren().removeAll(tfFormula, cbOperations, predicateBuilderView, symbolicModelCheckAlgorithmChoiceBox);
		switch (guiType) {
			case NONE:
				break;
			case TEXT_FIELD:
				formulaInput.getChildren().add(0, tfFormula);
				break;
			case CHOICE_BOX:
				formulaInput.getChildren().add(0, cbOperations);
				break;
			case PREDICATE:
				formulaInput.getChildren().add(0, predicateBuilderView);
				break;
			case SYMBOLIC_MODEL_CHECK_ALGORITHM:
				formulaInput.getChildren().add(0, symbolicModelCheckAlgorithmChoiceBox);
				break;
			default:
				throw new AssertionError("Unhandled GUI type: " + guiType);
		}
		this.sizeToScene();
	}
	
	protected String extractFormula() {
		String formula;
		if(this.getGUIType() == SymbolicGUIType.TEXT_FIELD) {
			formula = tfFormula.getText();
		} else if(this.getGUIType() == SymbolicGUIType.CHOICE_BOX) {
			if (this.checkAllOperations.equals(cbOperations.getSelectionModel().getSelectedItem())) {
				formula = "";
			} else {
				formula = cbOperations.getSelectionModel().getSelectedItem();
			}
		} else if(this.getGUIType() == SymbolicGUIType.PREDICATE) {
			formula = predicateBuilderView.getPredicate();
		} else if (this.getGUIType() == SymbolicGUIType.SYMBOLIC_MODEL_CHECK_ALGORITHM) {
			formula = symbolicModelCheckAlgorithmChoiceBox.getSelectionModel().getSelectedItem().name();
		} else {
			formula = "";
		}
		return formula;
	}

	protected abstract T extractItem();

	public void setData(T item) {
		cbChoice.getSelectionModel().select(item.getType());
		if(this.getGUIType() == SymbolicGUIType.TEXT_FIELD) {
			tfFormula.setText(item.getCode());
		} else if(this.getGUIType() == SymbolicGUIType.PREDICATE) {
			predicateBuilderView.setFromPredicate(item.getCode());
		} else if(this.getGUIType() == SymbolicGUIType.CHOICE_BOX) {
			if (item.getCode().isEmpty()) {
				cbOperations.getSelectionModel().select(this.checkAllOperations);
			} else {
				cbOperations.getSelectionModel().select(item.getCode());
			}
		} else if (this.getGUIType() == SymbolicGUIType.SYMBOLIC_MODEL_CHECK_ALGORITHM) {
			symbolicModelCheckAlgorithmChoiceBox.getSelectionModel().select(SymbolicModelcheckCommand.Algorithm.valueOf(item.getCode()));
		}
	}
	
	@FXML
	private void ok() {
		this.result = this.extractItem();
		this.close();
	}
	
	@FXML
	public void cancel() {
		this.result = null;
		this.close();
	}

	public T getResult() {
		return result;
	}
}
