package de.prob2.ui.project.preferences;

import com.google.inject.Injector;

import de.prob2.ui.internal.StageManager;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Text;

public class PreferenceView extends AnchorPane {
	
	@FXML
	private Label titelLabel;
	@FXML
	private Text prefText;
	
	private final Preference preference;
	private final Injector injector;
	
	PreferenceView(final Preference preference, final StageManager stageManager, final Injector injector) {
		this.preference = preference;
		this.injector = injector;
		stageManager.loadFXML(this, "preference_view.fxml");
	}

	@FXML
	public void initialize() {
		titelLabel.setText(preference.getName());
		
		StringBuilder builder = new StringBuilder();
		for(String pref : preference.getPreferences().keySet()) {
			builder.append(pref).append(" = ").append(preference.getPreferences().get(pref)).append("\n\n");
		}
		String prefs = builder.toString();
		prefText.setText(prefs);
	}

	@FXML
	public void closePreferenceView() {
		injector.getInstance(PreferencesTab.class).closePreferenceView();
	}

	Preference getPreference() {
		return this.preference;
	}
}
