package de.prob2.ui.visualisation;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.internal.StageManager;
import javafx.scene.layout.AnchorPane;

@Singleton
public class VisualisationView extends AnchorPane {

	@Inject
	public VisualisationView(final StageManager stageManager) {
		stageManager.loadFXML(this, "visualisation_view.fxml");
	}
}
