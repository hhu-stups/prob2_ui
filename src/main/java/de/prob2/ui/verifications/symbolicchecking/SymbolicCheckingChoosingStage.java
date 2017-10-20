package de.prob2.ui.verifications.symbolicchecking;

import com.google.inject.Singleton;


import de.prob2.ui.internal.StageManager;
import de.prob2.ui.verifications.symbolicchecking.SymbolicCheckingItem.GUIType;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import javax.inject.Inject;

@Singleton
public class SymbolicCheckingChoosingStage extends Stage {
		
	@FXML
	private SymbolicCheckingFormulaInput formulaInput;
	
	@FXML
	private ChoiceBox<SymbolicCheckingItem> cbChoice;
	
	@Inject
	private SymbolicCheckingChoosingStage(final StageManager stageManager) {
		stageManager.loadFXML(this, "symbolic_checking_choice.fxml");
		this.initModality(Modality.APPLICATION_MODAL);
	}
	
	@FXML
	public void initialize() {
		formulaInput.visibleProperty().bind(cbChoice.getSelectionModel().selectedItemProperty().isNotNull());
		cbChoice.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
			switch(newValue.getGUIType()) {
				case TEXT_FIELD:
					formulaInput.showTextField();
					break;
				case CHOICE_BOX:
					formulaInput.showChoiceBox();
					break;
				case NONE:
					formulaInput.showNone();
					break;
				default:
					break;
			}
		});
	}
	
	public GUIType getGUIType() {
		return cbChoice.getSelectionModel().getSelectedItem().getGUIType();
	}
	
	public SymbolicCheckingType getCheckingType() {
		return cbChoice.getSelectionModel().getSelectedItem().getCheckingType();
	}
	
}
