package de.prob2.ui.project.preferences;

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
	
	PreferenceView(final Preference preference, final StageManager stageManager) {
		this.preference = preference;
		stageManager.loadFXML(this, "preference_view.fxml");
	}

	@FXML
	public void initialize() {
		titelLabel.setText(preference.getName());
		
		StringBuilder builder = new StringBuilder();
		for(String pref : preference.getPreferences().keySet()) {
			builder.append(pref + " = " + preference.getPreferences().get(pref) + "\n\n");
		}
		String prefs = builder.toString();
		prefText.setText(prefs);
	}

}
