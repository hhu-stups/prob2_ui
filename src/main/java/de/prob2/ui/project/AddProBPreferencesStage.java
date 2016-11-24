package de.prob2.ui.project;

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

public class AddProBPreferencesStage extends Stage {
	private static final Logger logger = LoggerFactory.getLogger(AddProBPreferencesStage.class);

	@FXML
	private Button finishButton;
	@FXML
	private TextField nameField;

	AddProBPreferencesStage(FXMLLoader loader, CurrentStage currentStage) {
		try {
			loader.setLocation(getClass().getResource("add_probpreferences_stage.fxml"));
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
	}

	@FXML
	void cancel(ActionEvent event) {
		this.close();
	}

	@FXML
	void finish(ActionEvent event) {
		//TODO
		this.close();
	}
}
