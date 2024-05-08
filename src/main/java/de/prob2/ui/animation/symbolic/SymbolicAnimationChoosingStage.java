package de.prob2.ui.animation.symbolic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.inject.Inject;

import de.prob.statespace.LoadedMachine;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.sharedviews.PredicateBuilderTableItem;
import de.prob2.ui.sharedviews.PredicateBuilderView;
import de.prob2.ui.verifications.type.BuiltinValidationTaskTypes;
import de.prob2.ui.verifications.type.ValidationTaskType;

import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;

public class SymbolicAnimationChoosingStage extends Stage {
	@FXML
	private TextField tfFormula;
	
	@FXML
	private PredicateBuilderView predicateBuilderView;
	
	@FXML
	private VBox formulaInput;
	
	@FXML
	private ChoiceBox<ValidationTaskType<?>> cbChoice;
	
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
		cbChoice.setConverter(new StringConverter<>() {
			@Override
			public String toString(ValidationTaskType<?> object) {
				if (object == null) {
					return "";
				} else if (BuiltinValidationTaskTypes.CBC_FIND_SEQUENCE.equals(object)) {
					return i18n.translate("animation.type.sequence");
				} else if (BuiltinValidationTaskTypes.FIND_VALID_STATE.equals(object)) {
					return i18n.translate("animation.type.findValidState");
				} else {
					return object.getKey();
				}
			}
			
			@Override
			public ValidationTaskType<?> fromString(String string) {
				throw new UnsupportedOperationException("Conversion from String to ValidationTaskType not supported");
			}
		});
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
	
	public void changeGUIType(ValidationTaskType<?> type) {
		formulaInput.getChildren().removeAll(tfFormula, predicateBuilderView);
		if (BuiltinValidationTaskTypes.CBC_FIND_SEQUENCE.equals(type)) {
			formulaInput.getChildren().add(0, tfFormula);
		} else if (BuiltinValidationTaskTypes.FIND_VALID_STATE.equals(type)) {
			formulaInput.getChildren().add(0, predicateBuilderView);
		}
		this.sizeToScene();
	}
	
	public void setData(SymbolicAnimationItem item) {
		cbChoice.getSelectionModel().select(item.getTaskType());
		if (item instanceof CBCFindSequenceItem findSequenceItem) {
			tfFormula.setText(String.join(";", findSequenceItem.getOperationNames()));
		} else if (item instanceof FindValidStateItem findValidStateItem) {
			predicateBuilderView.setFromPredicate(findValidStateItem.getPredicate());
		} else {
			throw new AssertionError("Unhandled symbolic animation type: " + item.getClass());
		}
	}
	
	@FXML
	private void ok() {
		ValidationTaskType<?> type = cbChoice.getValue();
		if (BuiltinValidationTaskTypes.CBC_FIND_SEQUENCE.equals(type)) {
			List<String> operationNames = Arrays.asList(tfFormula.getText().replace(" ", "").split(";"));
			this.result = new CBCFindSequenceItem(operationNames);
		} else if (BuiltinValidationTaskTypes.FIND_VALID_STATE.equals(type)) {
			this.result = new FindValidStateItem(predicateBuilderView.getPredicate());
		} else {
			throw new AssertionError("Unhandled symbolic animation type: " + type);
		}
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
