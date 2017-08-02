package de.prob2.ui.project.machines;

import com.google.inject.Inject;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
	private Machine machine;

	@Inject
	public EditMachinesDialog(final StageManager stageManager, final CurrentProject currentProject) {
		super();
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
		stageManager.loadFXML(this, "machines_dialog.fxml");
	}

	public Optional<Machine> editAndShow(Machine machine) {
		this.setTitle("Edit " + machine.getName());
		this.machine = machine;

		List<Machine> machinesList = currentProject.getMachines();
		Set<String> machineNamesSet = new HashSet<>();
		machineNamesSet.addAll(machinesList.stream().map(Machine::getName).collect(Collectors.toList()));
		machineNamesSet.remove(machine.getName());

		nameField.textProperty().addListener((observable, from, to) -> {
			Button okButton = (Button) this.getDialogPane().lookupButton(okButtonType);
			if (machineNamesSet.contains(to)) {
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
