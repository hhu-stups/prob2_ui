package de.prob2.ui.animation;


import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import javafx.fxml.FXML;
import javafx.scene.control.TabPane;
import javafx.scene.layout.AnchorPane;

@FXMLInjected
@Singleton
public final class AnimationView extends AnchorPane {
	@FXML
	private TabPane tabPane;
			
	@Inject
	private AnimationView(final StageManager stageManager) {
		stageManager.loadFXML(this, "animationView.fxml");
	}
	
}
