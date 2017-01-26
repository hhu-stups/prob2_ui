package de.prob2.ui.machines;

import java.io.File;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.Machine;
import javafx.event.ActionEvent;
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
	private TextField nameField;
	@FXML
	private TextArea descriptionTextArea;
	@FXML
	private Label errorExplanationLabel;

	private File file;

	private Machine machine;

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

	@FXML
	void cancel(ActionEvent event) {
		this.machine = null;
		this.close();
	}

	@FXML
	void finish(ActionEvent event) {
		Path projectLocation = currentProject.getLocation().toPath();
		Path absolute = file.toPath();
		Path relative = projectLocation.relativize(absolute);
		machine = new Machine(nameField.getText(), descriptionTextArea.getText(), new File(relative.toString()));
		this.close();
	}
	
	public Machine addNewMachine(File machineFile, List<Machine> machinesList) {
		this.setTitle("Add new Machine");
		this.file = machineFile;
		for (Machine m : machinesList) {
			machineNamesSet.add(m.getName());
		}
		String[] n = machineFile.getName().split("\\.");
		String name = n[0];
		int i = 1;
		while (machineNamesSet.contains(name)) {
			name = n[0] + "(" + i + ")";
			i++;
		}
		nameField.setText(name);

		super.showAndWait();
		return machine;
	}
	
//	public Machine editMachine(MachineTableItem item, List<MachineTableItem> machinesList) {
//		machine = item.get();
//		this.file = machine.getLocation();
//		this.setTitle("Edit " + machine.getName());
//		for (MachineTableItem i : machinesList) {
//			if (item.equals(i)) {
//				machineNamesSet.add(i.getName());
//			}
//		}
//		nameField.setText(machine.getName());
//		descriptionField.setText(machine.getDescription());
//
//		super.showAndWait();
//		return machine;
//	}
}
