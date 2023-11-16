package de.prob2.ui.verifications.temporal;

import com.google.inject.Inject;

import de.prob.exception.ProBError;
import de.prob2.ui.codecompletion.ImprovedIntegerSpinnerValueFactory;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.layout.FontSize;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.verifications.temporal.ctl.CTLFormulaChecker;
import de.prob2.ui.verifications.temporal.ltl.formula.LTLFormulaChecker;
import de.prob2.ui.verifications.temporal.ltl.patterns.builtins.LTLBuiltinsStage;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.HBox;
import javafx.util.converter.IntegerStringConverter;

public class TemporalFormulaStage extends TemporalItemStage {

	@FXML
	private ChoiceBox<TemporalItemStage.TemporalFormulaChoiceItem> cbType;

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
	private Button btShowBuiltins;

	private final CurrentTrace currentTrace;
	private final IntegerProperty stateLimitProperty;

	private TemporalFormulaItem result;

	@Inject
	public TemporalFormulaStage(
		final StageManager stageManager, final CurrentProject currentProject, final CurrentTrace currentTrace, final FontSize fontSize,
		final LTLBuiltinsStage builtinsStage
	) {
		super(currentProject, fontSize, builtinsStage);
		this.currentTrace = currentTrace;
		this.stateLimitProperty = new SimpleIntegerProperty();
		this.result = null;
		stageManager.loadFXML(this, "temporal_formula_stage.fxml");
	}

	@Override
	public void initialize() {
		super.initialize();

		this.stateLimit.visibleProperty().bind(this.chooseStateLimit.selectedProperty());
		this.stateLimit.getEditor().setTextFormatter(new TextFormatter<>(new IntegerStringConverter()));
		this.stateLimit.setValueFactory(new ImprovedIntegerSpinnerValueFactory(1, Integer.MAX_VALUE, 500_000, 1_000));

		// bind the UI elements
		this.stateLimit.valueProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue != null && newValue >= 1) {
				this.stateLimitProperty.set(newValue);
			}
		});
		this.chooseStateLimit.selectedProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue != null && !newValue) {
				this.stateLimitProperty.set(-1);
			}
		});
		// reverse binding, used by "setData(TemporalFormulaItem)"
		this.stateLimitProperty.addListener((observable, oldValue, newValue) -> {
			if (newValue instanceof Integer) {
				int n = newValue.intValue();
				if (n >= 1) {
					this.stateLimit.getValueFactory().setValue(n);
					this.chooseStateLimit.setSelected(true);
				} else {
					this.chooseStateLimit.setSelected(false);
				}
			}
		});

		BooleanBinding binding = Bindings.createBooleanBinding(() -> cbType.getSelectionModel().selectedItemProperty().get() != null && cbType.getSelectionModel().selectedItemProperty().get().getType() == TemporalFormulaItem.TemporalType.LTL, cbType.getSelectionModel().selectedItemProperty());
		btShowBuiltins.visibleProperty().bind(binding);
		cbType.getSelectionModel().select(cbType.getItems().get(0));
		cbExpectedResult.getSelectionModel().select(true);
	}

	public void setData(final TemporalFormulaItem item) {
		this.cbType.setValue(new TemporalFormulaChoiceItem(item.getType()));
		this.idTextField.setText(item.getId() == null ? "" : item.getId());
		this.taCode.replaceText(item.getCode());
		this.taDescription.setText(item.getDescription());
		this.cbExpectedResult.setValue(item.getExpectedResult());
		this.stateLimitProperty.set(item.getStateLimit());
	}

	@FXML
	private void applyFormula() {
		result = null;
		final String id = idTextField.getText().trim().isEmpty() ? null : idTextField.getText();
		String code = taCode.getText();
		if (cbType.getValue().getType() == TemporalFormulaItem.TemporalType.LTL) {
			final TemporalFormulaItem item = new TemporalFormulaItem(TemporalFormulaItem.TemporalType.LTL, id, code, taDescription.getText(), this.stateLimitProperty.get(), cbExpectedResult.getValue());
			try {
				LTLFormulaChecker.parseFormula(item.getCode(), currentProject.getCurrentMachine(), currentTrace.getModel());
			} catch (ProBError e) {
				this.showErrors(e.getErrors());
				return;
			}
			result = item;
		} else if (cbType.getValue().getType() == TemporalFormulaItem.TemporalType.CTL) {
			final TemporalFormulaItem item = new TemporalFormulaItem(TemporalFormulaItem.TemporalType.CTL, id, code, taDescription.getText(), this.stateLimitProperty.get(), cbExpectedResult.getValue());
			try {
				CTLFormulaChecker.parseFormula(item.getCode(), currentTrace.getModel());
			} catch (ProBError e) {
				this.showErrors(e.getErrors());
				return;
			}
			result = item;
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
