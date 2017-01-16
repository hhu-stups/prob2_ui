package de.prob2.ui.project;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.prob2.ui.internal.StageManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class MachineStage extends Stage {
	@FXML
	private Button finishButton;
	@FXML
	private TextField nameField;
	@FXML
	private TextField descriptionField;
	@FXML
	private Label errorExplanationLabel;

	private File file;

	private Machine machine;

	private Set<String> machinesSet = new HashSet<>();

	MachineStage(StageManager stageManager) {
		stageManager.loadFXML(this, "add_machine_stage.fxml");
		this.initModality(Modality.APPLICATION_MODAL);
	}

	@FXML
	public void initialize() {
		nameField.textProperty().addListener((observable, from, to) -> {
			if (machinesSet.contains(to)) {
				finishButton.setDisable(true);
				errorExplanationLabel.setText("There is already a machine named '" + to + "'");
			} else if (to.isEmpty()) {
				finishButton.setDisable(true);
				errorExplanationLabel.setText("");
			} else {
				finishButton.setDisable(false);
				errorExplanationLabel.setText("");
			}
		});
	}

	@FXML
	void cancel(ActionEvent event) {
		this.close();
	}

	@FXML
	void finish(ActionEvent event) {
		machine = new Machine(nameField.getText(), descriptionField.getText(), file);
		this.close();
	}

	public Machine addNewMachine(File machineFile, List<MachineTableItem> machinesList) {
		this.setTitle("Add new Machine");
		this.file = machineFile;
		for (MachineTableItem i : machinesList) {
			machinesSet.add(i.getName());
		}
		String[] n = machineFile.getName().split("\\.");
		String name = n[0];
		int i = 1;
		while (machinesSet.contains(name)) {
			name = n[0] + "(" + i++ + ")";
		}
		nameField.setText(name);

		super.showAndWait();
		return machine;
	}

	public Machine editMachine(MachineTableItem item, List<MachineTableItem> machinesList) {
		machine = item.get();
		this.file = machine.getLocation();
		this.setTitle("Edit " + machine.getName());
		for (MachineTableItem i : machinesList) {
			if (i != item) {
				machinesSet.add(i.getName());
			}
		}
		nameField.setText(machine.getName());
		descriptionField.setText(machine.getDescription());

		super.showAndWait();
		return machine;
	}
}
