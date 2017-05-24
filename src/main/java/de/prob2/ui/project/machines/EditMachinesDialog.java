package de.prob2.ui.project.machines;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.inject.Inject;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.runconfigurations.Runconfiguration;
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

	private final CurrentProject currentProject;
	private Machine editMachine;

	@Inject
	public EditMachinesDialog(final StageManager stageManager, final CurrentProject currentProject) {
		super();
		this.currentProject = currentProject;

		this.setResultConverter(type -> {
			if (type == null || type.getButtonData() == ButtonBar.ButtonData.CANCEL_CLOSE) {
				return null;
			} else {
				List<Runconfiguration> runconfigList = currentProject.getRunconfigurations();
				runconfigList.stream().filter(runconfig -> runconfig.getMachine().equals(editMachine.getName()))
						.forEach(runconfig -> currentProject.runconfigurationsProperty().set(
								runconfigList.indexOf(runconfig),
								new Runconfiguration(nameField.getText(), runconfig.getPreference())));
				editMachine.setName(nameField.getText());
				editMachine.setDescription(descriptionTextArea.getText());
				return editMachine;
			}
		});
		stageManager.loadFXML(this, "machines_dialog.fxml");
	}

	public Optional<Machine> editAndShow(Machine machine) {
		this.setTitle("Edit " + machine.getName());
		editMachine = machine;

		List<Machine> machinesList = currentProject.getMachines();
		Set<String> machineNamesSet = new HashSet<>();
		machineNamesSet.addAll(machinesList.stream().map(Machine::getName).collect(Collectors.toList()));
		machineNamesSet.remove(machine.getName());

		nameField.textProperty().addListener((observable, from, to) -> {
			Button okButton = (Button) this.getDialogPane().lookupButton(okButtonType);
			if (machineNamesSet.contains((String) to)) {
				okButton.setDisable(true);
				errorExplanationLabel.setText("There is already a machine named '" + to + "'");
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
