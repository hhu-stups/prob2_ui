package de.prob2.ui.symbolic;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ResourceBundle;

import de.prob.animator.command.SymbolicModelcheckCommand;
import de.prob.statespace.LoadedMachine;
import de.prob2.ui.internal.AbstractResultHandler;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.sharedviews.PredicateBuilderView;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public abstract class SymbolicChoosingStage<T extends SymbolicItem> extends Stage {
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
	
	@FXML
	private ChoiceBox<SymbolicModelcheckCommand.Algorithm> symbolicModelCheckAlgorithmChoiceBox;
	
	@FXML
	private VBox formulaInput;
	
	@FXML
	private ChoiceBox<SymbolicExecutionItem> cbChoice;
	
	private final ResourceBundle bundle;
	
	protected final CurrentProject currentProject;
	
	private final CurrentTrace currentTrace;
	
	private final SymbolicFormulaHandler<T> formulaHandler;
	
	private final String checkAllOperations;
	
	public SymbolicChoosingStage(final ResourceBundle bundle, final CurrentProject currentProject, final CurrentTrace currentTrace, final SymbolicFormulaHandler<T> formulaHandler) {
		this.bundle = bundle;
		this.currentProject = currentProject;
		this.currentTrace = currentTrace;
		this.formulaHandler = formulaHandler;
		this.checkAllOperations = bundle.getString("verifications.symbolicchecking.choice.checkAllOperations");
		
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
			changeGUIType(to.getGUIType());
			this.sizeToScene();
		});
		symbolicModelCheckAlgorithmChoiceBox.getItems().setAll(SymbolicModelcheckCommand.Algorithm.values());
		symbolicModelCheckAlgorithmChoiceBox.getSelectionModel().select(0);
	}
	
	public SymbolicGUIType getGUIType() {
		return cbChoice.getSelectionModel().getSelectedItem().getGUIType();
	}
	
	public SymbolicExecutionType getExecutionType() {
		return cbChoice.getSelectionModel().getSelectedItem().getExecutionType();
	}
	
	public void select(SymbolicItem item) {
		cbChoice.getItems().forEach(choice -> {
			if(item.getType().equals(choice.getExecutionType())) {
				cbChoice.getSelectionModel().select(choice);
			}
		});
	}
	
	public void reset() {
		btAdd.setText(bundle.getString("common.buttons.add"));
		btCheck.setText(bundle.getString("symbolic.formulaInput.buttons.addAndCheck"));
		setCheckListeners();
		tfFormula.clear();
		predicateBuilderView.reset();
		cbOperations.getSelectionModel().select(this.checkAllOperations);
		cbChoice.getSelectionModel().clearSelection();
		symbolicModelCheckAlgorithmChoiceBox.getSelectionModel().select(0);
	}
	
	protected void update() {
		cbOperations.getItems().setAll(this.checkAllOperations);
		cbOperations.getSelectionModel().select(this.checkAllOperations);
		final Map<String, String> items = new LinkedHashMap<>();
		if (currentTrace.get() != null) {
			final LoadedMachine loadedMachine = currentTrace.getStateSpace().getLoadedMachine();
			if (loadedMachine != null) {
				cbOperations.getItems().addAll(loadedMachine.getOperationNames());
				loadedMachine.getConstantNames().forEach(s -> items.put(s, ""));
				loadedMachine.getVariableNames().forEach(s -> items.put(s, ""));
			}
		}
		predicateBuilderView.setItems(items);
	}
	
	protected void setCheckListeners() {
		btAdd.setOnAction(e -> addFormula());
		btCheck.setOnAction(e -> checkFormula());
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
			formula = this.getExecutionType().name();
		}
		return formula;
	}

	protected abstract T extractItem();

	public void changeFormula(T item, AbstractResultHandler resultHandler) {
		btAdd.setText(bundle.getString("symbolic.formulaInput.buttons.change"));
		btCheck.setText(bundle.getString("symbolic.formulaInput.buttons.changeAndCheck"));
		setChangeListeners(item, resultHandler);
		this.select(item);
		if(this.getGUIType() == SymbolicGUIType.TEXT_FIELD) {
			tfFormula.setText(item.getCode());
		} else if(this.getGUIType() == SymbolicGUIType.PREDICATE) {
			predicateBuilderView.setFromPredicate(item.getName());
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
	
	protected void setChangeListeners(T item, AbstractResultHandler resultHandler) {
		btAdd.setOnAction(e -> {
			final T newItem = this.extractItem();
			if(this.formulaHandler.replaceFormula(item, newItem)) {
				addFormula();
			} else {
				resultHandler.showAlreadyExists(AbstractResultHandler.ItemType.CONFIGURATION);
			}
			this.close();
		});
		
		btCheck.setOnAction(e -> {
			final T newItem = this.extractItem();
			if(this.formulaHandler.replaceFormula(item, newItem)) {
				checkFormula();
			} else {
				resultHandler.showAlreadyExists(AbstractResultHandler.ItemType.CONFIGURATION);
			}
			this.close();
		});
	}
	
	private void checkFormula() {
		addFormula();
		this.formulaHandler.handleItem(this.extractItem(), false);
		this.close();
	}
	
	private void addFormula() {
		this.formulaHandler.addFormula(this.extractItem());
		this.close();
	}
	
	@FXML
	public void cancel() {
		this.close();
	}
}
