package de.prob2.ui.verifications.symbolicchecking;

import javax.inject.Inject;

import com.google.inject.Singleton;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.verifications.symbolicchecking.SymbolicCheckingItem.GUIType;

import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

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
		cbChoice.getSelectionModel().selectedItemProperty().addListener((o, from, to) -> {
			if(to == null) {
				return;
			}
			switch(to.getGUIType()) {
				case NONE:
					formulaInput.showNone();
					break;
				case TEXT_FIELD:
					formulaInput.showTextField();
					break;
				case CHOICE_BOX:
					formulaInput.showChoiceBox();
					break;
				case PREDICATE:
					formulaInput.showPredicate();
					break;
				default:
					throw new AssertionError("Unhandled GUI type: " + to.getGUIType());
			}
		});
	}
	
	public GUIType getGUIType() {
		return cbChoice.getSelectionModel().getSelectedItem().getGUIType();
	}
	
	public SymbolicCheckingType getCheckingType() {
		return cbChoice.getSelectionModel().getSelectedItem().getCheckingType();
	}
	
	public void select(SymbolicCheckingFormulaItem item) {
		cbChoice.getItems().forEach(choice -> {
			if(item.getType().equals(choice.getCheckingType())) {
				cbChoice.getSelectionModel().select(choice);
			}
		});
	}
	
	public void reset() {
		formulaInput.reset();
		cbChoice.getSelectionModel().clearSelection();
	}
	
}
