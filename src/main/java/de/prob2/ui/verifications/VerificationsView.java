package de.prob2.ui.verifications;

import com.google.inject.Inject;

import de.prob2.ui.internal.StageManager;
import javafx.scene.layout.AnchorPane;

public class VerificationsView extends AnchorPane {
		
	@Inject
	private VerificationsView(final StageManager stageManager) {
		stageManager.loadFXML(this, "verificationsView.fxml");
	}
	
}
