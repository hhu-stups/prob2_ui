package de.prob2.ui.dynamic;

import java.util.Set;

import de.prob.animator.domainobjects.ErrorItem;
import de.prob2.ui.internal.ExtendedCodeArea;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

public abstract class EditDynamicFormulaStage<T extends DynamicFormulaTask<T>> extends Stage {

	@FXML
	private Label formulaTitleLabel;
	@FXML
	private TextField idField;
	@FXML
	private ExtendedCodeArea formulaTextArea;
	@FXML
	private Label errorExplanationLabel;
	@FXML
	private Button okButton;
	@FXML
	private Button cancelButton;

	private final I18n i18n;
	private final CurrentProject currentProject;
	private String command;
	private T result;

	public EditDynamicFormulaStage(final StageManager stageManager, final I18n i18n, final CurrentProject currentProject) {
		super();
		this.i18n = i18n;
		this.currentProject = currentProject;
		stageManager.loadFXML(this, "edit_formula_stage.fxml");
	}

	@FXML
	private void initialize() {
		this.formulaTitleLabel.setText(this.i18n.translate("dynamic.editFormula"));
		this.formulaTextArea.setOnKeyPressed(e -> {
			if (e.getCode().equals(KeyCode.ENTER)) {
				if (!e.isShiftDown()) {
					this.okButton.fire();
					e.consume();
				} else {
					this.formulaTextArea.insertText(this.formulaTextArea.getCaretPosition(), "\n");
				}
			}
		});
		this.idField.textProperty().addListener((observable, from, to) -> {
			Set<String> idList = this.currentProject.getCurrentMachine().getMachineProperties().getValidationTaskIds();
			if (idList.contains(to)) {
				this.okButton.setDisable(true);
				this.errorExplanationLabel.setText(i18n.translate("dynamic.editFormula.IdAlreadyExistsError", to));
			} else {
				this.okButton.setDisable(false);
				this.errorExplanationLabel.setText("");
			}
		});

		this.okButton.setOnAction(e -> {
			setResult();
			this.close();
		});
		this.cancelButton.setOnAction(e -> this.close());
	}

	public void createNewItem(String command) {
		this.idField.clear();
		this.formulaTextArea.clear();
		this.command = command;
	}

	public void setInitialTask(T item, ObservableList<ErrorItem> errors) {
		this.command = item.getCommandType();
		this.idField.setText(item.getId() != null ? item.getId() : "");
		this.formulaTextArea.replaceText(item.getFormula());
		// TODO: does this make sense?
		this.formulaTextArea.getErrors().setAll(errors);
	}

	protected abstract T createNewItem(String id, String command, String formula);

	private void setResult() {
		final String formula = formulaTextArea.getText().trim();
		final String id = idField.getText().trim().isEmpty() ? null : idField.getText();
		this.result = createNewItem(id, command, formula);
	}

	public T getResult() {
		return this.result;
	}
}
