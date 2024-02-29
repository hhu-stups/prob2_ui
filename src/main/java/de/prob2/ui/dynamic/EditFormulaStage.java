package de.prob2.ui.dynamic;

import com.google.inject.Inject;
import de.prob.animator.domainobjects.ErrorItem;
import de.prob2.ui.internal.ExtendedCodeArea;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.vomanager.IValidationTask;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

import java.util.Set;
import java.util.stream.Collectors;

public class EditFormulaStage extends Stage {
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
	DynamicCommandFormulaItem item;
	private final CurrentProject currentProject;
	private String lastItemCommand;

	@Inject
	public EditFormulaStage(final StageManager stageManager, final I18n i18n, final CurrentProject currentProject) {
		super();
		this.i18n = i18n;
		this.currentProject = currentProject;
		stageManager.loadFXML(this, "edit_formula_stage.fxml");
	}

	@FXML
	private void initialize() {
		formulaTextArea.setOnKeyPressed(e -> {
			if (e.getCode().equals(KeyCode.ENTER)) {
				if (!e.isShiftDown()) {
					okButton.fire();
					e.consume();
				} else {
					formulaTextArea.insertText(formulaTextArea.getCaretPosition(), "\n");
				}
			}
		});

		this.okButton.setOnAction(e -> {
			setItem();
			this.close();
		});
		this.cancelButton.setOnAction(e -> this.close());
	}

	public void createNewItem(String lastItemCommand){
		this.lastItemCommand = lastItemCommand;
		checkId();
	}

	public void editItem(DynamicCommandFormulaItem item, ObservableList<ErrorItem> errors){
		this.item = item;
		if (item.getId() != null) {
			this.idField.appendText(item.getId());
			this.idField.setEditable(false);
		} else {
			checkId();
		}
		this.formulaTextArea.appendText(item.getFormula());
		formulaTextArea.getErrors().setAll(errors);
		this.formulaTitleLabel.setText(i18n.translate("dynamic.editFormula"));
	}

	private void checkId() {
		Set<String> idList = currentProject.getCurrentMachine().getMachineProperties().validationTasksOldProperty().get().values().stream()
			                     .map(IValidationTask::getId)
			                     .collect(Collectors.toSet());
		idField.textProperty().addListener((observable, from, to) -> {
			if (idList.contains(to)) {
				okButton.setDisable(true);
				errorExplanationLabel.setText(i18n.translate("dynamic.editFormula.IdAlreadyExistsError", to));
			} else {
				okButton.setDisable(false);
				errorExplanationLabel.setText("");
			}
		});
	}

	private void setItem() {
		final String formula = formulaTextArea.getText().trim();
		final String id = idField.getText().trim().isEmpty() ? null : idField.getText();
		if (item == null) {
			this.item = new DynamicCommandFormulaItem(id, lastItemCommand, formula);
		} else {
			item.setId(id);
			item.setFormula(formula);
		}
	}

	public DynamicCommandFormulaItem getItem() {
		return this.item;
	}

}
