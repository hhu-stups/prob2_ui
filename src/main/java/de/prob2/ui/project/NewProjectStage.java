package de.prob2.ui.project;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import com.google.inject.Inject;

import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
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

	private final FileChooserManager fileChooserManager;
	private final CurrentProject currentProject;
	private final I18n i18n;
	private final StageManager stageManager;

	@Inject
	private NewProjectStage(final FileChooserManager fileChooserManager, CurrentProject currentProject, StageManager stageManager, I18n i18n) {
		this.fileChooserManager = fileChooserManager;
		this.currentProject = currentProject;
		this.i18n = i18n;
		this.initModality(Modality.APPLICATION_MODAL);
		this.stageManager = stageManager;
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
		dirChooser.setTitle(i18n.translate("project.newProjectStage.directoryChooser.selectLocation.title"));
		final Path path = fileChooserManager.showDirectoryChooser(dirChooser, null, this.getOwner());
		if (path != null) {
			locationField.setText(path.toString());
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
			stageManager.makeAlert(Alert.AlertType.ERROR, "", "project.newProjectStage.invalidLocationError").show();
			return;
		}
		Project newProject = new Project(projectNameField.getText(), projectDescriptionTextArea.getText(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Project.metadataBuilder().build(), dir);
		boolean replacingProject = currentProject.confirmReplacingProject();
		if(replacingProject) {
			currentProject.switchTo(newProject, true);
		}
		this.close();
	}
}
