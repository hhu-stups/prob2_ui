package de.prob2.ui.symbolic;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import de.prob.animator.command.SymbolicModelcheckCommand;
import de.prob.statespace.LoadedMachine;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.sharedviews.PredicateBuilderTableItem;
import de.prob2.ui.sharedviews.PredicateBuilderView;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public abstract class SymbolicChoosingStage<T extends SymbolicItem<ET>, ET extends SymbolicExecutionType> extends Stage {
	@FXML
	private Button btCheck;
	
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
	
	protected final CurrentProject currentProject;
	
	private final CurrentTrace currentTrace;
	
	private final SymbolicFormulaHandler<T> formulaHandler;
	
	private final String checkAllOperations;

	private T lastItem;
	
	public SymbolicChoosingStage(final I18n i18n, final CurrentProject currentProject, final CurrentTrace currentTrace, final SymbolicFormulaHandler<T> formulaHandler) {
		this.i18n = i18n;
		this.currentProject = currentProject;
		this.currentTrace = currentTrace;
		this.formulaHandler = formulaHandler;
		this.checkAllOperations = i18n.translate("verifications.symbolicchecking.choice.checkAllOperations");
		
		this.initModality(Modality.APPLICATION_MODAL);
	}
	
	@FXML
	public void initialize() {
		this.update();
		currentTrace.addListener((observable, from, to) -> update());
		setCheckListeners();
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
	
	protected void setCheckListeners() {
		btCheck.setOnAction(e -> {
			final T newItem = this.extractItem();
			final Optional<T> existingItem = this.formulaHandler.addItem(currentProject.getCurrentMachine(), newItem);
			lastItem = existingItem.orElse(newItem);
			this.close();
			this.formulaHandler.handleItem(lastItem, false);
		});
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

	public void changeFormula(T item) {
		btCheck.setText(i18n.translate("symbolic.formulaInput.buttons.change"));
		setChangeListeners(item);
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
		this.show();
	}
	
	protected void setChangeListeners(T item) {
		btCheck.setOnAction(e -> {
			final T newItem = this.extractItem();
			final Optional<T> existingItem = this.formulaHandler.replaceItem(currentProject.getCurrentMachine(), item, newItem);
			lastItem = existingItem.orElse(newItem);
			this.close();
			this.formulaHandler.handleItem(lastItem, false);
		});
	}
	
	@FXML
	public void cancel() {
		this.close();
	}

	public T getLastItem() {
		return lastItem;
	}
}
