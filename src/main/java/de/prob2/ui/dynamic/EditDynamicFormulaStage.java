package de.prob2.ui.dynamic;

import java.util.Set;

import com.google.inject.Inject;

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

public final class EditDynamicFormulaStage extends Stage {
	@FunctionalInterface
	public interface DynamicFormulaTaskFactory {
		VisualizationFormulaTask createTask(String id, String formula);
	}

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

	private DynamicFormulaTaskFactory factory;
	private VisualizationFormulaTask result;

	@Inject
	public EditDynamicFormulaStage(final StageManager stageManager, final I18n i18n, final CurrentProject currentProject) {
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
			Set<String> idList = this.currentProject.getCurrentMachine().getValidationTaskIds();
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

	public void createNewFormulaTask(DynamicFormulaTaskFactory factory) {
		this.idField.clear();
		this.formulaTextArea.clear();
		this.formulaTextArea.getErrors().clear();
		this.factory = factory;
	}

	public void setInitialFormulaTask(VisualizationFormulaTask item, ObservableList<ErrorItem> errors, DynamicFormulaTaskFactory factory) {
		this.idField.setText(item.getId() != null ? item.getId() : "");
		this.formulaTextArea.replaceText(item.getFormula());
		this.formulaTextArea.getErrors().setAll(errors);
		this.factory = factory;
	}

	private void setResult() {
		String id = idField.getText().trim().isEmpty() ? null : idField.getText();
		String formula = formulaTextArea.getText().trim();
		this.result = this.factory.createTask(id, formula);
	}

	public VisualizationFormulaTask getResult() {
		return this.result;
	}
}
