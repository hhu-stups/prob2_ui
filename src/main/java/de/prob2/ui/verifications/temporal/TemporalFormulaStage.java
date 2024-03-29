package de.prob2.ui.verifications.temporal;

import com.google.inject.Inject;

import de.prob.exception.ProBError;
import de.prob2.ui.internal.ImprovedIntegerSpinnerValueFactory;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.layout.FontSize;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.verifications.temporal.ctl.CTLFormulaChecker;
import de.prob2.ui.verifications.temporal.ltl.formula.LTLFormulaChecker;
import de.prob2.ui.verifications.temporal.ltl.patterns.builtins.LTLBuiltinsStage;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
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

	private TemporalFormulaItem result;

	@Inject
	public TemporalFormulaStage(
		final StageManager stageManager, final CurrentProject currentProject, final CurrentTrace currentTrace, final FontSize fontSize,
		final LTLBuiltinsStage builtinsStage
	) {
		super(currentProject, fontSize, builtinsStage);
		this.currentTrace = currentTrace;
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
		BooleanBinding binding = Bindings.createBooleanBinding(() -> cbType.getSelectionModel().selectedItemProperty().get() != null && cbType.getSelectionModel().selectedItemProperty().get().getType() == TemporalFormulaType.LTL, cbType.getSelectionModel().selectedItemProperty());
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
		if (item.getStateLimit() >= 1) {
			this.stateLimit.getValueFactory().setValue(item.getStateLimit());
			this.chooseStateLimit.setSelected(true);
		} else {
			this.chooseStateLimit.setSelected(false);
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

	@FXML
	private void applyFormula() {
		result = null;
		final String id = idTextField.getText().trim().isEmpty() ? null : idTextField.getText();
		String code = taCode.getText();
		TemporalFormulaType type = cbType.getValue().getType();
		if (type == TemporalFormulaType.LTL) {
			final TemporalFormulaItem item = new TemporalFormulaItem(type, id, code, taDescription.getText(), this.getStateLimit(), cbExpectedResult.getValue());
			try {
				LTLFormulaChecker.parseFormula(item.getCode(), currentProject.getCurrentMachine(), currentTrace.getModel());
			} catch (ProBError e) {
				this.showErrors(e.getErrors());
				return;
			}
			result = item;
		} else if (type == TemporalFormulaType.CTL) {
			final TemporalFormulaItem item = new TemporalFormulaItem(type, id, code, taDescription.getText(), this.getStateLimit(), cbExpectedResult.getValue());
			try {
				CTLFormulaChecker.parseFormula(item.getCode(), currentTrace.getModel());
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
