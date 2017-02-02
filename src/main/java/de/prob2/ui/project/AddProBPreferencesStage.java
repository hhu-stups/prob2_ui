package de.prob2.ui.project;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class AddProBPreferencesStage extends Stage {
	@FXML
	private Button finishButton;
	@FXML
	private Button cancelButton;
	@FXML
	private Button addPreferenceButton;
	@FXML
	private TextField nameField;
	@FXML
	private TextField preferenceNameField;
	@FXML
	private TextField preferenceValueField;
	@FXML
	private ListView<Map.Entry<String, String>> preferencesListView;
	@FXML
	private Label errorExplanationLabel;

	private Map<String, String> preferenceMap = new HashMap<>();

	private Set<String> preferenceNamesSet = new HashSet<>();
	private CurrentProject currentProject;

	AddProBPreferencesStage(StageManager stageManager, CurrentProject currentProject) {
		this.currentProject = currentProject;
		stageManager.loadFXML(this, "add_probpreferences_stage.fxml");
		this.initModality(Modality.APPLICATION_MODAL);
	}

	@FXML
	public void initialize() {
		addPreferenceButton.disableProperty().bind(preferenceNameField.lengthProperty().lessThanOrEqualTo(0)
				.or(preferenceValueField.lengthProperty().lessThanOrEqualTo(0)));
		nameField.textProperty().addListener((observable, from, to) -> {
			if (preferenceNamesSet.contains(to)) {
				finishButton.setDisable(true);
				errorExplanationLabel.setText("There is already a preference named '" + to + "'");
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
	void addPreference(ActionEvent event) {
		preferenceMap.put(preferenceNameField.getText(), preferenceValueField.getText());
		preferencesListView.getItems().clear();
		preferencesListView.getItems().addAll(preferenceMap.entrySet());
	}

	public void showStage() {
		List<Preference> preferencesList = currentProject.getPreferences();
		preferenceNamesSet.addAll(preferencesList.stream().map(Preference::getName).collect(Collectors.toList()));

		finishButton.setOnAction(event -> {
			Preference preference = new Preference(nameField.getText(), preferenceMap);
			currentProject.addPreference(preference);
			this.close();
		});
		
		cancelButton.setOnAction(event -> this.close());
		super.showAndWait();
	}
}
