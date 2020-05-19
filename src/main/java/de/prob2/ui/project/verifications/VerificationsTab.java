package de.prob2.ui.project.verifications;

import com.google.inject.Inject;

import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import javafx.fxml.FXML;
import javafx.scene.control.Tab;

@FXMLInjected
public class VerificationsTab extends Tab {
	@FXML HelpButton helpButton;

	@Inject
	private VerificationsTab(final StageManager stageManager) {
		stageManager.loadFXML(this, "verifications_tab.fxml");
	}
	
	@FXML
	public void initialize() {
		helpButton.setHelpContent("project", "Verifications");
	}
}
