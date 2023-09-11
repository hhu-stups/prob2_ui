package de.prob2.ui.dynamic;

import com.google.inject.Inject;
import de.prob2.ui.internal.ExtendedCodeArea;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.Machine;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class EditFormulaDialog extends Dialog<DynamicCommandFormulaItem> {

	@FXML
	private Label formulaTitleLabel;
	@FXML
	private TextField idField;
	@FXML
	private ExtendedCodeArea formulaTextArea;
	@FXML
	private Label errorExplanationLabel;
	@FXML
	private ButtonType okButtonType;

	private final I18n i18n;
	private Machine machine;

	@Inject
	public EditFormulaDialog(final StageManager stageManager, final I18n i18n, final CurrentProject currentProject) {
		super();
		this.i18n = i18n;
		this.machine = currentProject.getCurrentMachine();
		stageManager.loadFXML(this, "edit_formula_dialog.fxml");
	}

	public Optional<DynamicCommandFormulaItem> editAndShow(CurrentProject currentProject, TableRow<DynamicCommandFormulaItem> row, String lastItemCommand) {
		if(row.getItem().getId()!=null){
			this.idField.appendText(row.getItem().getId());
			this.idField.setEditable(false);
		} else {
			checkIDs(currentProject);
		}
		this.formulaTextArea.appendText(row.getItem().getFormula());
		this.formulaTitleLabel.setText(i18n.translate("dynamic.editFormula"));

		this.setResultConverter(type -> {
			if (type == null || type.getButtonData() == ButtonBar.ButtonData.CANCEL_CLOSE) {
				return null;
			} else {
				final String formula = formulaTextArea.getText().trim();
				final String id = idField.getText().trim().isEmpty() ? null : idField.getText();
				if (formula.isEmpty()) {
					return null;
				}
				DynamicCommandFormulaItem item = row.getItem();
				item.setId(id);
				item.setFormula(formula);
				return item;
			}
		});
		return super.showAndWait();
	}

	public Optional<DynamicCommandFormulaItem> addAndShow(CurrentProject currentProject, String lastItemCommand) {
		checkIDs(currentProject);
		this.setResultConverter(type -> {
			if (type == null || type.getButtonData() == ButtonBar.ButtonData.CANCEL_CLOSE) {
				return null;
			} else {
				final String formula = formulaTextArea.getText().trim();
				final String id = idField.getText().trim().isEmpty() ? null : idField.getText();
				if (formula.isEmpty()){
					return null;
				}
				return new DynamicCommandFormulaItem(id, lastItemCommand, formula);
			}
		});
		return super.showAndWait();
	}

	private void checkIDs(CurrentProject currentProject){
		List<String> idList = currentProject.getCurrentMachine().getValidationTasks().values().stream().map(e->e.getId()).collect(Collectors.toList());

		idField.textProperty().addListener((observable, from, to) -> {
			Button okButton = (Button) this.getDialogPane().lookupButton(okButtonType);
			if (idList.contains(to)) {
				okButton.setDisable(true);
				errorExplanationLabel.setText(i18n.translate("dynamic.editFormula.IdAlreadyExistsError", to));
			} else {
				okButton.setDisable(false);
				errorExplanationLabel.setText("");
			}
		});
	}
}
