package de.prob2.ui.project.machines;

import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.inject.Inject;

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

public class AddMachinesDialog extends Dialog<Machine> {
	@FXML
	private TextField nameField;
	@FXML
	private TextArea descriptionTextArea;
	@FXML
	private Label errorExplanationLabel;
	@FXML
	private ButtonType okButtonType;

	private final CurrentProject currentProject;
	private Path machinePath;
	private Machine.Type machineType;

	@Inject
	public AddMachinesDialog(final StageManager stageManager, final CurrentProject currentProject) {
		super();
		this.currentProject = currentProject;
				
		this.setResultConverter(type -> {
			if (type == null || type.getButtonData() == ButtonBar.ButtonData.CANCEL_CLOSE) {
				return null;
			} else {
				return new Machine(nameField.getText(), descriptionTextArea.getText(), machinePath, machineType);
			}
		});
		stageManager.loadFXML(this, "machines_dialog.fxml");
	}
	
	public Optional<Machine> showAndWait(Path machinePath, Machine.Type machineType) {
		this.machinePath = machinePath;
		this.machineType = machineType;
		final Set<String> machineNamesSet = currentProject.getMachines().stream().map(Machine::getName).collect(Collectors.toSet());
		
		String[] n = machinePath.toFile().getName().split("\\.");
		String name = n[0];
		int i = 1;
		while (machineNamesSet.contains(name)) {
			name = n[0] + "(" + i + ")";
			i++;
		}
		nameField.setText(name);
		
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
		return showAndWait();
	}
}
