package de.prob2.ui.project.preferences;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.inject.Inject;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.preferences.GlobalPreferences;
import de.prob2.ui.preferences.PreferencesView;
import de.prob2.ui.preferences.ProBPreferences;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.MachineLoader;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class PreferencesDialog extends Dialog<Preference> {
	@FXML
	private TextField nameField;
	@FXML
	private PreferencesView prefsView;
	@FXML
	private ButtonType okButtonType;
	@FXML
	private Label errorExplanationLabel;

	private final ProBPreferences prefs;
	private final CurrentProject currentProject;
	private Preference preference;
	private Set<String> preferencesNamesSet;

	@Inject
	private PreferencesDialog(final StageManager stageManager, final MachineLoader machineLoader, final ProBPreferences prefs,
			final GlobalPreferences globalPreferences, CurrentProject currentProject) {
		super();

		this.currentProject = currentProject;

		this.prefs = prefs;
		this.prefs.setStateSpace(machineLoader.getEmptyStateSpace(globalPreferences));

		this.setResultConverter(type -> {
			if (type == null || type.getButtonData() == ButtonBar.ButtonData.CANCEL_CLOSE) {
				return null;
			} else {
				preference.setName(this.nameField.getText());
				preference.setPreferences(new HashMap<>(this.prefs.getChangedPreferences()));
				return preference; 
			}
		});

		stageManager.loadFXML(this, "preferences_dialog.fxml");
	}

	@FXML
	private void initialize() {
		this.prefsView.setPreferences(this.prefs);
		this.preference = new Preference("", new HashMap<>());

		List<Preference> preferencesList = currentProject.getPreferences();
		preferencesNamesSet = preferencesList.stream().map(Preference::getName).collect(Collectors.toCollection(HashSet::new));

		Button okButton = (Button) this.getDialogPane().lookupButton(okButtonType);
		okButton.setDisable(true);
		nameField.textProperty().addListener((observable, from, to) -> {
			if (preferencesNamesSet.contains(to)) {
				okButton.setDisable(true);
				errorExplanationLabel.setText("There is already a preference named '" + to + "'");
			} else if ("default".equals(to)) {
				okButton.setDisable(true);
				errorExplanationLabel.setText("Name cannot be 'default'");
			} else if (to.isEmpty()) {
				okButton.setDisable(true);
				errorExplanationLabel.setText("Name cannot be empty");
			} else {
				okButton.setDisable(false);
				errorExplanationLabel.setText("");
			}
		});
	}

	void setPreference(Preference preference) {
		this.preference = preference;
		preferencesNamesSet.remove(preference.getName());
		this.nameField.setText(preference.getName());
		for (Map.Entry<String, String> pref : preference.getPreferences().entrySet()) {
			this.prefs.setPreferenceValue(pref.getKey(), pref.getValue());
		}
	}
}
