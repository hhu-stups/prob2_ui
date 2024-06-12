package de.prob2.ui.verifications.temporal;

import com.google.inject.Inject;

import de.prob.exception.ProBError;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.ImprovedIntegerSpinnerValueFactory;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.layout.FontSize;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.verifications.temporal.ctl.CTLFormulaParser;
import de.prob2.ui.verifications.temporal.ctl.CTLFormulaItem;
import de.prob2.ui.verifications.temporal.ltl.LTLFormulaItem;
import de.prob2.ui.verifications.temporal.ltl.formula.LTLFormulaParser;
import de.prob2.ui.verifications.temporal.ltl.patterns.builtins.LTLBuiltinsStage;
import de.prob2.ui.verifications.type.BuiltinValidationTaskTypes;
import de.prob2.ui.verifications.type.ValidationTaskType;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;
import javafx.util.converter.IntegerStringConverter;

public final class TemporalFormulaStage extends TemporalItemStage {
	@FXML
	private ChoiceBox<ValidationTaskType<?>> cbType;

	@FXML
	private TextField idTextField;

	@FXML
	private Button applyButton;

	@FXML
	private HBox expectedResultBox;

	@FXML
	private ChoiceBox<Boolean> cbExpectedResult;

	@FXML
	private CheckBox chooseStateLimit;

	@FXML
	private Spinner<Integer> stateLimit;

	@FXML
	private RadioButton startStateAllInitialStates;

	@FXML
	private RadioButton startStateCurrentState;

	@FXML
	private Button btShowBuiltins;

	private final CurrentProject currentProject;

	private final CurrentTrace currentTrace;

	private final I18n i18n;

	private TemporalFormulaItem result;

	@Inject
	public TemporalFormulaStage(
		final StageManager stageManager, final CurrentProject currentProject, final CurrentTrace currentTrace, final I18n i18n, final FontSize fontSize,
		final LTLBuiltinsStage builtinsStage
	) {
		super(fontSize, builtinsStage);
		this.currentProject = currentProject;
		this.currentTrace = currentTrace;
		this.i18n = i18n;
		this.result = null;
		stageManager.loadFXML(this, "temporal_formula_stage.fxml");
	}

	@Override
	public void initialize() {
		super.initialize();

		cbType.setConverter(new StringConverter<>() {
			@Override
			public String toString(ValidationTaskType<?> object) {
				if (object == BuiltinValidationTaskTypes.LTL) {
					return i18n.translate("verifications.temporal.type.ltl");
				} else if (object == BuiltinValidationTaskTypes.CTL) {
					return i18n.translate("verifications.temporal.type.ctl");
				} else {
					throw new AssertionError("Unhandled temporal formula type: " + object);
				}
			}

			@Override
			public ValidationTaskType<? extends TemporalFormulaItem> fromString(String string) {
				throw new UnsupportedOperationException("Conversion from String to ValidationTaskType not supported");
			}
		});

		this.stateLimit.visibleProperty().bind(this.chooseStateLimit.selectedProperty());
		this.stateLimit.getEditor().setTextFormatter(new TextFormatter<>(new IntegerStringConverter()));
		this.stateLimit.setValueFactory(new ImprovedIntegerSpinnerValueFactory(1, Integer.MAX_VALUE, 500_000, 1_000));

		// bind the UI elements
		BooleanBinding binding = Bindings.createBooleanBinding(() -> cbType.getSelectionModel().selectedItemProperty().get() != null && cbType.getSelectionModel().selectedItemProperty().get() == BuiltinValidationTaskTypes.LTL, cbType.getSelectionModel().selectedItemProperty());
		btShowBuiltins.visibleProperty().bind(binding);
		cbType.getSelectionModel().select(cbType.getItems().get(0));
		cbExpectedResult.getSelectionModel().select(true);
	}

	public void setData(final TemporalFormulaItem item) {
		this.cbType.setValue(item.getTaskType());
		this.idTextField.setText(item.getId() == null ? "" : item.getId());
		this.taCode.replaceText(item.getCode());
		this.taDescription.setText(item.getDescription());
		this.cbExpectedResult.setValue(item.getExpectedResult());
		if (item.getStateLimit() >= 1) {
			this.stateLimit.getValueFactory().setValue(item.getStateLimit());
			this.chooseStateLimit.setSelected(true);
		} else {
			this.chooseStateLimit.setSelected(false);
		}

		switch (item.getStartState()) {
			case ALL_INITIAL_STATES:
				this.startStateAllInitialStates.setSelected(true);
				break;

			case CURRENT_STATE:
				this.startStateCurrentState.setSelected(true);
				break;

			default:
				throw new AssertionError("Unhandled start state type: " + item.getStartState());
		}
	}

	private int getStateLimit() {
		if (this.chooseStateLimit.isSelected()) {
			Integer stateLimit = this.stateLimit.getValue();
			if (stateLimit != null && stateLimit >= 1) {
				return stateLimit;
			}
		}

		return -1;
	}

	private TemporalFormulaItem.StartState getStartState() {
		if (this.startStateAllInitialStates.isSelected()) {
			return TemporalFormulaItem.StartState.ALL_INITIAL_STATES;
		} else if (this.startStateCurrentState.isSelected()) {
			return TemporalFormulaItem.StartState.CURRENT_STATE;
		} else {
			throw new AssertionError("No start state selected?!");
		}
	}

	@FXML
	private void applyFormula() {
		result = null;
		final String id = idTextField.getText().trim().isEmpty() ? null : idTextField.getText();
		String code = taCode.getText();
		ValidationTaskType<?> type = cbType.getValue();
		if (type == BuiltinValidationTaskTypes.LTL) {
			LTLFormulaItem item = new LTLFormulaItem(id, code, taDescription.getText(), this.getStateLimit(), this.getStartState(), cbExpectedResult.getValue());
			try {
				LTLFormulaParser.parseFormula(item.getCode(), currentProject.getCurrentMachine(), currentTrace.getModel());
			} catch (ProBError e) {
				this.showErrors(e.getErrors());
				return;
			}
			result = item;
		} else if (type == BuiltinValidationTaskTypes.CTL) {
			CTLFormulaItem item = new CTLFormulaItem(id, code, taDescription.getText(), this.getStateLimit(), this.getStartState(), cbExpectedResult.getValue());
			try {
				CTLFormulaParser.parseFormula(item.getCode(), currentTrace.getModel());
			} catch (ProBError e) {
				this.showErrors(e.getErrors());
				return;
			}
			result = item;
		} else {
			throw new AssertionError();
		}

		this.close();
	}

	@FXML
	private void cancel() {
		this.result = null;
		this.close();
	}

	public TemporalFormulaItem getResult() {
		return this.result;
	}
}
