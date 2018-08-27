package de.prob2.ui.verifications.symbolicchecking;

import javax.inject.Inject;

import com.google.inject.Singleton;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.symbolic.SymbolicExecutionItem;
import de.prob2.ui.symbolic.SymbolicExecutionType;
import de.prob2.ui.symbolic.SymbolicGUIType;

import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

@Singleton
public class SymbolicCheckingChoosingStage extends Stage {
		
	@FXML
	private SymbolicCheckingFormulaInput formulaInput;
	
	@FXML
	private ChoiceBox<SymbolicExecutionItem> cbChoice;
	
	@Inject
	private SymbolicCheckingChoosingStage(final StageManager stageManager) {
		stageManager.loadFXML(this, "symbolic_checking_choice.fxml");
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
	
	public SymbolicExecutionType getCheckingType() {
		return cbChoice.getSelectionModel().getSelectedItem().getExecutionType();
	}
	
	public void select(SymbolicCheckingFormulaItem item) {
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
