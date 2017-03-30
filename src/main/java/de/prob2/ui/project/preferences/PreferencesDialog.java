package de.prob2.ui.project.preferences;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.inject.Inject;

import de.prob.scripting.Api;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.preferences.PreferencesView;
import de.prob2.ui.preferences.ProBPreferences;
import de.prob2.ui.prob2fx.CurrentProject;
import javafx.beans.property.SimpleBooleanProperty;
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
	private final ResourceBundle bundle;
	private final CurrentProject currentProject;

	@Inject
	private PreferencesDialog(final StageManager stageManager, final Api api, final ProBPreferences prefs,
			final ResourceBundle bundle, CurrentProject currentProject) {
		super();

		this.currentProject = currentProject;

		this.prefs = prefs;
		this.prefs.setStateSpace(ProBPreferences.getEmptyStateSpace(api));

		this.bundle = bundle;

		this.setResultConverter(type -> {
			if (type == null || type.getButtonData() == ButtonBar.ButtonData.CANCEL_CLOSE) {
				return null;
			} else {
				return new Preference(this.nameField.getText(), new HashMap<>(this.prefs.getChangedPreferences()));
			}
		});

		stageManager.loadFXML(this, "preferences_dialog.fxml");
	}

	@FXML
	private void initialize() {
		this.prefsView.setPreferences(this.prefs);
		this.setTitle(bundle.getString("addProBPreference.stage.stageTitle"));

		List<Preference> preferencesList = currentProject.getPreferences();
		Set<String> preferencesNamesSet = new HashSet<>();
		preferencesNamesSet.addAll(preferencesList.stream().map(Preference::getName).collect(Collectors.toList()));

		nameField.textProperty().addListener((observable, from, to) -> {
			Button okButton = (Button) this.getDialogPane().lookupButton(okButtonType);
			if (preferencesNamesSet.contains(to)) {
				okButton.setDisable(true);
				errorExplanationLabel.setText("There is already a preference named '" + to + "'");
			} else if (to.isEmpty()) {
				okButton.setDisable(true);
				errorExplanationLabel.setText("");
			} else {
				okButton.setDisable(false);
				errorExplanationLabel.setText("");
			}
		});
	}

	void setPreference(Preference preference) {
		nameField.setText(preference.getName() + "(copy)");
		for (Entry<String, String> pref : preference.getPreferences().entrySet()) {
			prefs.setPreferenceValue(pref.getKey(), pref.getValue());
		}
	}
}
