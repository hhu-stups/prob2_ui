package de.prob2.ui.project.machines;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.inject.Inject;

import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class EditMachinesDialog extends Dialog<Machine> {
	@FXML
	private TextField nameField;
	@FXML
	private TextArea descriptionTextArea;
	@FXML
	private Label errorExplanationLabel;
	@FXML
	private ButtonType okButtonType;

	private final I18n i18n;
	private final CurrentProject currentProject;
	private Machine machine;

	@Inject
	public EditMachinesDialog(final StageManager stageManager, final I18n i18n, final CurrentProject currentProject) {
		super();
		this.i18n = i18n;
		this.currentProject = currentProject;

		this.setResultConverter(type -> {
			if (type == null || type.getButtonData() == ButtonBar.ButtonData.CANCEL_CLOSE) {
				return null;
			} else {
				machine.setName(nameField.getText());
				machine.setDescription(descriptionTextArea.getText());
				return machine;
			}
		});
		stageManager.loadFXML(this, "edit_machines_dialog.fxml");
	}

	public Optional<Machine> editAndShow(Machine machine) {
		this.setTitle(i18n.translate("project.machines.editMachinesDialog.title", machine.getName()));
		this.machine = machine;

		List<Machine> machinesList = currentProject.getMachines();
		Set<String> machineNamesSet = machinesList.stream().map(Machine::getName).collect(Collectors.toSet());
		machineNamesSet.remove(machine.getName());

		nameField.textProperty().addListener((observable, from, to) -> {
			Button okButton = (Button) this.getDialogPane().lookupButton(okButtonType);
			if (machineNamesSet.contains(to)) {
				okButton.setDisable(true);
				errorExplanationLabel.setText(i18n.translate("project.machines.editMachinesDialog.machineAlreadyExistsError", to));
			} else if (to.isEmpty()) {
				okButton.setDisable(true);
				errorExplanationLabel.setText("");
			} else {
				okButton.setDisable(false);
				errorExplanationLabel.setText("");
			}
		});

		nameField.setText(machine.getName());
		descriptionTextArea.setText(machine.getDescription());

		return super.showAndWait();
	}
}
