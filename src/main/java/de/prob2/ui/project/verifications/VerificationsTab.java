package de.prob2.ui.project.verifications;

import com.google.inject.Inject;

import de.prob2.ui.internal.StageManager;
import javafx.scene.control.Tab;

public class VerificationsTab extends Tab {
	
	@Inject
	private VerificationsTab(final StageManager stageManager) {
		stageManager.loadFXML(this, "verifications_tab.fxml");
	}
	
	
}
