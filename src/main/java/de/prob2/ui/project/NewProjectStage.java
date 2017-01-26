package de.prob2.ui.project;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.google.inject.Inject;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
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
	private ListView<Preference> preferencesListView;
	@FXML
	private Label errorExplanationLabel;

	private CurrentProject currentProject;
	private Map<String, Preference> preferencesMap = new HashMap<>();
	private StageManager stageManager;

	@Inject
	private NewProjectStage(CurrentProject currentProject, StageManager stageManager) {
		this.currentProject = currentProject;
		this.stageManager = stageManager;
		this.initModality(Modality.APPLICATION_MODAL);
		stageManager.loadFXML(this, "new_project_stage.fxml");
	}

	@FXML
	public void initialize() {
		finishButton.disableProperty().bind(projectNameField.lengthProperty().lessThanOrEqualTo(0));
		locationField.setText(this.currentProject.getDefaultLocation().toString());
	}

	@FXML
	void addPreference(ActionEvent event) {
		AddProBPreferencesStage addProBPreferencesStage = new AddProBPreferencesStage(stageManager);
		Preference preference = addProBPreferencesStage.showStage(preferencesMap.keySet());
		if (preference != null) {
			preferencesListView.getItems().add(preference);
			preferencesMap.put(preference.toString(), preference);
		}
	}

	@FXML
	void selectLocation(ActionEvent event) {
		DirectoryChooser dirChooser = new DirectoryChooser();
		dirChooser.setTitle("Select Location");
		File file = dirChooser.showDialog(this.getOwner());		
		if(file != null) {
			locationField.setText(file.getAbsolutePath());
		}
	}

	@FXML
	void cancel(ActionEvent event) {
		this.close();
	}

	@FXML
	void finish(ActionEvent event) {
		File dir = new File(locationField.getText());
		if (!dir.isDirectory()) {
			errorExplanationLabel.setText("The location does not exist or is invalid");
			return;
		}
		Map<String, Preference> preferences = preferencesMap;
		Project newProject = new Project(projectNameField.getText(), projectDescriptionTextArea.getText(), new ArrayList<>(),
				preferences, dir);
		currentProject.set(newProject);
		currentProject.save();
		this.close();
	}
}
