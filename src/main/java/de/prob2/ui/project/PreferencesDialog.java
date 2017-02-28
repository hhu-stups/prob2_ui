package de.prob2.ui.project;

import java.util.HashMap;

import com.google.inject.Inject;

import de.prob.scripting.Api;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.preferences.PreferencesView;
import de.prob2.ui.preferences.ProBPreferences;
import javafx.fxml.FXML;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextField;

public class PreferencesDialog extends Dialog<Preference> {
	@FXML private TextField nameField;
	@FXML private PreferencesView prefsView;
	
	private final ProBPreferences prefs;
	
	@Inject
	private PreferencesDialog(final StageManager stageManager, final Api api, final ProBPreferences prefs) {
		super();
		
		this.prefs = prefs;
		this.prefs.setStateSpace(ProBPreferences.getEmptyStateSpace(api));
		
		this.setResultConverter(type -> {
			if (type == null || type.getButtonData() == ButtonBar.ButtonData.CANCEL_CLOSE) {
				return null;
			} else {
				return new Preference(
					this.nameField.getText(),
					new HashMap<>(this.prefs.getChangedPreferences())
				);
			}
		});
		
		stageManager.loadFXML(this, "preferences_dialog.fxml");
	}
	
	@FXML
	private void initialize() {
		this.prefsView.setPreferences(this.prefs);
		this.setTitle("Add ProB Preference");
	}
}
