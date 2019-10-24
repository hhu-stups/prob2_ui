package de.prob2.ui.project;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.ResourceBundle;

import com.google.inject.Inject;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class NewProjectStage extends Stage {
	@FXML
	private Button finishButton;
	@FXML
	private TextField projectNameField;
	@FXML
	private TextArea projectDescriptionTextArea;
	@FXML
	private TextField locationField;
	@FXML
	private Label errorExplanationLabel;

	private final CurrentProject currentProject;
	private final ResourceBundle bundle;

	@Inject
	private NewProjectStage(CurrentProject currentProject, StageManager stageManager, ResourceBundle bundle) {
		this.currentProject = currentProject;
		this.bundle = bundle;
		this.initModality(Modality.APPLICATION_MODAL);
		stageManager.loadFXML(this, "new_project_stage.fxml");
	}

	@FXML
	public void initialize() {
		finishButton.disableProperty().bind(projectNameField.lengthProperty().lessThanOrEqualTo(0));
		locationField.setText(this.currentProject.getDefaultLocation().toString());
	}
	
	@FXML
	void selectLocation() {
		DirectoryChooser dirChooser = new DirectoryChooser();
		dirChooser.setTitle(bundle.getString("project.newProjectStage.directoryChooser.selectLocation.title"));
		File file = dirChooser.showDialog(this.getOwner());
		if (file != null) {
			locationField.setText(file.getAbsolutePath());
		}
	}

	@FXML
	void cancel() {
		this.close();
	}

	@FXML
	void finish() {
		Path dir = Paths.get(locationField.getText());
		if (!dir.toFile().isDirectory()) {
			errorExplanationLabel.setText(bundle.getString("project.newProjectStage.invalidLocationError"));
			return;
		}
		Project newProject = new Project(projectNameField.getText(), projectDescriptionTextArea.getText(), Collections.emptyList(), Collections.emptyList(), dir);
		boolean replacingProject = currentProject.confirmReplacingProject();
		if(replacingProject) {
			currentProject.switchTo(newProject, true);
		}
		this.close();
	}
}
