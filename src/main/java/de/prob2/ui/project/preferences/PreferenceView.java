package de.prob2.ui.project.preferences;

import com.google.inject.Injector;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.layout.FontSize;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Text;

public class PreferenceView extends AnchorPane {
	
	@FXML
	private Label titelLabel;
	@FXML
	private Text prefText;
	@FXML
	private Button closePreferenceViewButton;
	
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
			builder.append(pref + " = " + preference.getPreferences().get(pref) + "\n\n");
		}
		String prefs = builder.toString();
		prefText.setText(prefs);
		
		FontSize fontsize = injector.getInstance(FontSize.class);
		((FontAwesomeIconView) (closePreferenceViewButton.getGraphic())).glyphSizeProperty().bind(fontsize);
	}

	@FXML
	public void closePreferenceView() {
		injector.getInstance(PreferencesTab.class).closePreferenceView();
	}

	Preference getPreference() {
		return this.preference;
	}
}
