package de.prob2.ui.project;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.prob2.ui.prob2fx.CurrentStage;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class AddMachineStage extends Stage {
	private static final Logger logger = LoggerFactory.getLogger(AddMachineStage.class);

	@FXML
	private Button finishButton;
	@FXML
	private Button addMachineButton;
	@FXML
	private TextField nameField;
	@FXML
	private TextField descriptionField;

	private File machineFile;

	private Machine machine;

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
		currentStage.register(this);
	}

	@FXML
	public void initialize() {
		finishButton.disableProperty().bind(nameField.lengthProperty().lessThanOrEqualTo(0));
		String name[] = machineFile.getName().split("\\.");
		nameField.setText(name[0]);
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

	@FXML
	void addMachine(ActionEvent event) {
		//TODO
	}

	public Machine showStage() {
		super.showAndWait();
		return machine;
	}
}
