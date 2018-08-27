package de.prob2.ui.animation.symbolic;

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
public class SymbolicAnimationChoosingStage extends Stage {

	@FXML
	private SymbolicAnimationFormulaInput formulaInput;
	
	@FXML
	private ChoiceBox<SymbolicExecutionItem> cbChoice;
	
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
					break;
			}
		});
		
	}
	
	public SymbolicGUIType getGUIType() {
		return cbChoice.getSelectionModel().getSelectedItem().getGUIType();
	}
	
	public SymbolicExecutionType getAnimationType() {
		return cbChoice.getSelectionModel().getSelectedItem().getExecutionType();
	}
	
	public void select(SymbolicAnimationFormulaItem item) {
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
