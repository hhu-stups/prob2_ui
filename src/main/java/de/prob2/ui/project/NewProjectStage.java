package de.prob2.ui.project;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.dotty.DottyStage;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentStage;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;

@Singleton
public class NewProjectStage extends Stage {
	private static final Logger logger = LoggerFactory.getLogger(DottyStage.class);
	
	@FXML private Button finishButton;
	@FXML private TextField projectNameField;

	private CurrentProject currentProject;

	@Inject
	private NewProjectStage(FXMLLoader loader, CurrentProject currentProject, CurrentStage currentStage) {
		this.currentProject = currentProject;
		try {
			loader.setLocation(getClass().getResource("new_project_stage.fxml"));
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
		finishButton.disableProperty().bind(projectNameField.lengthProperty().lessThanOrEqualTo(0));
	}
	
	@FXML
	void cancel(ActionEvent event) {
		this.close();
	}
	
	@FXML
	void finish(ActionEvent event) {
		Project newProject = new Project(projectNameField.getText());
		currentProject.changeCurrentProjet(newProject);
		this.close();
	}
}
