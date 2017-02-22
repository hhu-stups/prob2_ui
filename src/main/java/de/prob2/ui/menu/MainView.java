package de.prob2.ui.menu;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.internal.StageManager;
import javafx.scene.layout.AnchorPane;
@Singleton
public class MainView extends AnchorPane {
	
	@Inject
	private MainView(StageManager stageManager) {
		stageManager.loadFXML(this, "mainView.fxml");
	}

}
