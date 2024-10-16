package de.prob2.ui.project.preferences;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.inject.Inject;

import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.preferences.GlobalPreferences;
import de.prob2.ui.preferences.PreferencesChangeState;
import de.prob2.ui.preferences.PreferencesView;
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

	private final I18n i18n;
	private final PreferencesChangeState state;
	private final CurrentProject currentProject;
	private Set<String> preferencesNamesSet;

	@Inject
	private PreferencesDialog(final StageManager stageManager, final I18n i18n, final MachineLoader machineLoader, final GlobalPreferences globalPreferences, CurrentProject currentProject) {
		super();

		this.i18n = i18n;
		this.currentProject = currentProject;

		this.state = new PreferencesChangeState(machineLoader.getActiveStateSpace().getPreferenceInformation());
		this.state.setCurrentPreferenceValues(globalPreferences);

		this.setResultConverter(type -> {
			if (type == null || type.getButtonData() == ButtonBar.ButtonData.CANCEL_CLOSE) {
				return null;
			} else {
				return new Preference(this.nameField.getText(), new HashMap<>(this.state.getPreferenceChanges()));
			}
		});

		stageManager.loadFXML(this, "preferences_dialog.fxml");
	}

	@FXML
	private void initialize() {
		this.setResizable(true);
		this.prefsView.setState(this.state);

		List<Preference> preferencesList = currentProject.getPreferences();
		preferencesNamesSet = preferencesList.stream()
			.map(Preference::getName)
			.collect(Collectors.toCollection(HashSet::new));

		Button okButton = (Button) this.getDialogPane().lookupButton(okButtonType);
		okButton.setDisable(true);
		nameField.textProperty().addListener((observable, from, to) -> {
			if (preferencesNamesSet.contains(to)) {
				okButton.setDisable(true);
				errorExplanationLabel.setText(i18n.translate("project.preferences.preferencesDialog.errorLabel.preferenceAlreadyExists", to));
			} else if (Preference.DEFAULT.getName().equals(to)) {
				okButton.setDisable(true);
				errorExplanationLabel.setText(i18n.translate("project.preferences.preferencesDialog.errorLabel.nameCannotBeDefault"));
			} else if (to.isEmpty()) {
				okButton.setDisable(true);
				errorExplanationLabel.setText(i18n.translate("project.preferences.preferencesDialog.errorLabel.nameCannotBeEmpty"));
			} else {
				okButton.setDisable(false);
				errorExplanationLabel.setText("");
			}
		});
	}

	void setPreference(Preference preference) {
		this.setTitle(i18n.translate("project.preferences.preferencesDialog.editPreferenceTitle", preference.getName()));
		preferencesNamesSet.remove(preference.getName());
		this.nameField.setText(preference.getName());
		for (Map.Entry<String, String> pref : preference.getPreferences().entrySet()) {
			this.state.changePreference(pref.getKey(), pref.getValue());
		}
		this.prefsView.refresh();
	}
}
