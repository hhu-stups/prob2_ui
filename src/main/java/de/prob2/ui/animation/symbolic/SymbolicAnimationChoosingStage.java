package de.prob2.ui.animation.symbolic;

import javax.inject.Inject;

import com.google.inject.Singleton;

import de.prob2.ui.animation.symbolic.SymbolicAnimationItem.GUIType;
import de.prob2.ui.internal.StageManager;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

@Singleton
public class SymbolicAnimationChoosingStage extends Stage {

	@FXML
	private SymbolicAnimationFormulaInput formulaInput;
	
	@FXML
	private ChoiceBox<SymbolicAnimationItem> cbChoice;
	
	@Inject
	private SymbolicAnimationChoosingStage(final StageManager stageManager) {
		stageManager.loadFXML(this, "symbolic_animation_choice.fxml");
		this.initModality(Modality.APPLICATION_MODAL);
	}
	
	@FXML
	public void initialize() {
		formulaInput.visibleProperty().bind(cbChoice.getSelectionModel().selectedItemProperty().isNotNull());
		cbChoice.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
			if(newValue == null) {
				return;
			}
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
	
	public SymbolicAnimationType getAnimationType() {
		return cbChoice.getSelectionModel().getSelectedItem().getAnimationType();
	}
	
	public void select(SymbolicAnimationFormulaItem item) {
		cbChoice.getItems().forEach(choice -> {
			if(item.getType().equals(choice.getAnimationType())) {
				cbChoice.getSelectionModel().select(choice);
			}
		});
	}
	
	public void reset() {
		formulaInput.reset();
		cbChoice.getSelectionModel().clearSelection();
	}
	
	
}
