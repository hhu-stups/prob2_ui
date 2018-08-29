package de.prob2.ui.symbolic;

import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public abstract class SymbolicChoosingStage extends Stage {
		
	@FXML
	private SymbolicFormulaInput formulaInput;
	
	@FXML
	private ChoiceBox<SymbolicExecutionItem> cbChoice;
	
	
	public SymbolicChoosingStage() {
		this.initModality(Modality.APPLICATION_MODAL);
	}
	
	@FXML
	public void initialize() {
		formulaInput.visibleProperty().bind(cbChoice.getSelectionModel().selectedItemProperty().isNotNull());
		cbChoice.getSelectionModel().selectedItemProperty().addListener((o, from, to) -> {
			formulaInput.changeGUIType(to == null ? null : to.getGUIType());
			this.sizeToScene();
		});
	}
	
	public SymbolicGUIType getGUIType() {
		return cbChoice.getSelectionModel().getSelectedItem().getGUIType();
	}
	
	public SymbolicExecutionType getExecutionType() {
		return cbChoice.getSelectionModel().getSelectedItem().getExecutionType();
	}
	
	public void select(SymbolicFormulaItem item) {
		cbChoice.getItems().forEach(choice -> {
			if(item.getType().equals(choice.getExecutionType())) {
				cbChoice.getSelectionModel().select(choice);
			}
		});
	}
	
	public void reset() {
		formulaInput.reset();
		cbChoice.getSelectionModel().clearSelection();
	}
}