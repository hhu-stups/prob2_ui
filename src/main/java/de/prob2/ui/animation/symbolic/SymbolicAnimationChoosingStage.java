package de.prob2.ui.animation.symbolic;

import java.util.ArrayList;
import java.util.List;

import com.google.inject.Inject;

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

public class SymbolicAnimationChoosingStage extends Stage {
	@FXML
	private TextField tfFormula;
	
	@FXML
	private PredicateBuilderView predicateBuilderView;
	
	@FXML
	private VBox formulaInput;
	
	@FXML
	private ChoiceBox<SymbolicAnimationType> cbChoice;
	
	private final I18n i18n;
	
	private SymbolicAnimationItem result;
	
	@Inject
	private SymbolicAnimationChoosingStage(final StageManager stageManager, final I18n i18n) {
		this.i18n = i18n;
		
		this.initModality(Modality.APPLICATION_MODAL);
		stageManager.loadFXML(this, "symbolic_animation_choice.fxml");
	}
	
	@FXML
	public void initialize() {
		formulaInput.visibleProperty().bind(cbChoice.getSelectionModel().selectedItemProperty().isNotNull());
		cbChoice.getSelectionModel().selectedItemProperty().addListener((o, from, to) -> {
			if(to == null) {
				return;
			}
			changeGUIType(to);
			this.sizeToScene();
		});
		cbChoice.setConverter(i18n.translateConverter());
		this.setResizable(true);
	}
	
	public void setMachine(final LoadedMachine loadedMachine) {
		final List<PredicateBuilderTableItem> items = new ArrayList<>();
		if (loadedMachine != null) {
			loadedMachine.getConstantNames().forEach(s -> items.add(new PredicateBuilderTableItem(s, "", PredicateBuilderTableItem.VariableType.CONSTANT)));
			loadedMachine.getVariableNames().forEach(s -> items.add(new PredicateBuilderTableItem(s, "", PredicateBuilderTableItem.VariableType.VARIABLE)));
		}
		predicateBuilderView.setItems(items);
	}
	
	public void changeGUIType(final SymbolicAnimationType type) {
		formulaInput.getChildren().removeAll(tfFormula, predicateBuilderView);
		switch (type) {
			case SEQUENCE:
				formulaInput.getChildren().add(0, tfFormula);
				break;
			
			case FIND_VALID_STATE:
				formulaInput.getChildren().add(0, predicateBuilderView);
				break;
			
			default:
				throw new AssertionError("Unhandled symbolic animation type: " + type);
		}
		this.sizeToScene();
	}
	
	protected String extractFormula() {
		return switch (cbChoice.getValue()) {
			case SEQUENCE -> tfFormula.getText();
			case FIND_VALID_STATE -> predicateBuilderView.getPredicate();
		};
	}
	
	public void setData(SymbolicAnimationItem item) {
		cbChoice.getSelectionModel().select(item.getType());
		switch (item.getType()) {
			case SEQUENCE:
				tfFormula.setText(item.getCode());
				break;
			
			case FIND_VALID_STATE:
				predicateBuilderView.setFromPredicate(item.getCode());
				break;
			
			default:
				throw new AssertionError("Unhandled symbolic animation type: " + cbChoice.getValue());
		}
	}
	
	@FXML
	private void ok() {
		this.result = new SymbolicAnimationItem(this.extractFormula(), cbChoice.getSelectionModel().getSelectedItem());
		this.close();
	}
	
	@FXML
	public void cancel() {
		this.result = null;
		this.close();
	}
	
	public SymbolicAnimationItem getResult() {
		return result;
	}
}
