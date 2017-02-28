package de.prob2.ui.project;

import java.io.File;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class MachineStage extends Stage {
	@FXML
	private Button finishButton;
	@FXML
	private Button cancelButton;
	@FXML
	private TextField nameField;
	@FXML
	private TextArea descriptionTextArea;
	@FXML
	private Label errorExplanationLabel;

	private Set<String> machineNamesSet = new HashSet<>();
	private CurrentProject currentProject;

	public MachineStage(StageManager stageManager, CurrentProject currentProject) {
		this.currentProject = currentProject;
		stageManager.loadFXML(this, "add_machine_stage.fxml");
		this.initModality(Modality.APPLICATION_MODAL);
	}

	@FXML
	public void initialize() {
		nameField.textProperty().addListener((observable, from, to) -> {
			if (machineNamesSet.contains(to)) {
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

	public void addNewMachine(File machineFile) {
		this.setTitle("Add Machine");
		List<Machine> machinesList = currentProject.getMachines();
		machineNamesSet.addAll(machinesList.stream().map(Machine::getName).collect(Collectors.toList()));
		
		String[] n = machineFile.getName().split("\\.");
		String name = n[0];
		int i = 1;
		while (machineNamesSet.contains(name)) {
			name = n[0] + "(" + i + ")";
			i++;
		}
		nameField.setText(name);

		finishButton.setOnAction(event -> {
			Path projectLocation = currentProject.getLocation().toPath();
			Path absolute = machineFile.toPath();
			Path relative = projectLocation.relativize(absolute);
			Machine machine = new Machine(nameField.getText(), descriptionTextArea.getText(), relative);
			currentProject.addMachine(machine);
			this.close();
		});
		
		cancelButton.setOnAction(event -> this.close());
		super.showAndWait();
	}

	public void editMachine(Machine machine) {
		this.setTitle("Edit " + machine.getName());	
		List<Machine> machinesList = currentProject.getMachines();
		machineNamesSet.clear();
		machineNamesSet.addAll(machinesList.stream().map(Machine::getName).collect(Collectors.toList()));
		machineNamesSet.remove(machine.getName());

		nameField.setText(machine.getName());
		descriptionTextArea.setText(machine.getDescription());

		finishButton.setOnAction(event -> {
			Machine edited = new Machine(nameField.getText(), descriptionTextArea.getText(), machine.getPreferences(), machine.getPath());
			currentProject.removeMachine(machine);
			currentProject.addMachine(edited);
			this.close();
		});
		
		cancelButton.setOnAction(event -> this.close());
		
		super.showAndWait();
	}
}
