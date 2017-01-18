package de.prob2.ui.project;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import de.prob2.ui.internal.StageManager;
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

	private Preference preference;

	private Set<String> preferencesSet;

	AddProBPreferencesStage(StageManager stageManager) {
		stageManager.loadFXML(this, "add_probpreferences_stage.fxml");
		this.initModality(Modality.APPLICATION_MODAL);
	}

	@FXML
	public void initialize() {
		addPreferenceButton.disableProperty().bind(preferenceNameField.lengthProperty().lessThanOrEqualTo(0)
				.or(preferenceValueField.lengthProperty().lessThanOrEqualTo(0)));
		nameField.textProperty().addListener((observable, from, to) -> {
			if (preferencesSet.contains(to)) {
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

	@FXML
	void cancel(ActionEvent event) {
		this.close();
	}

	@FXML
	void finish(ActionEvent event) {
		preference = new Preference(nameField.getText(), preferenceMap);
		this.close();
	}

	public Preference showStage(Set<String> preferencesList) {
		this.preferencesSet = preferencesList;
		super.showAndWait();
		return preference;
	}
}
