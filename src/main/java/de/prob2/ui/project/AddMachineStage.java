package de.prob2.ui.project;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.prob2.ui.prob2fx.CurrentStage;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class AddMachineStage extends Stage {
	private static final Logger logger = LoggerFactory.getLogger(AddMachineStage.class);

	@FXML
	private Button finishButton;
	@FXML
	private TextField nameField;
	@FXML
	private TextField descriptionField;
	@FXML
	private Label errorExplanationLabel;

	private File machineFile;

	private Machine machine;

	private Set<String> machinesSet = new HashSet<String>();

	AddMachineStage(FXMLLoader loader, CurrentStage currentStage, File machineFile) {
		this.machineFile = machineFile;
		try {
			loader.setLocation(getClass().getResource("add_machine_stage.fxml"));
			loader.setRoot(this);
			loader.setController(this);
			loader.load();
		} catch (IOException e) {
			logger.error("loading fxml failed", e);
		}
		this.initModality(Modality.WINDOW_MODAL);
		this.initOwner(currentStage.get());
		currentStage.register(this, this.getClass().getName());
	}

	@FXML
	public void initialize() {
		nameField.textProperty().addListener((observable, from, to) -> {
			if (machinesSet.contains(to)) {
				finishButton.setDisable(true);
				errorExplanationLabel.setText("There is already a machine named '" + to + "'");
			} else if (to.equals("")) {
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
		machine = new Machine(nameField.getText(), descriptionField.getText(), machineFile);
		this.close();
	}

	public Machine showStage(List<MachineTableItem> machinesList) {
		for(MachineTableItem i: machinesList) {
			machinesSet.add(i.getName());
		}
		String name[] = machineFile.getName().split("\\.");	
		nameField.setText(name[0]);
		super.showAndWait();
		return machine;
	}
}
