package de.prob2.ui.menu;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.internal.StageManager;
import javafx.fxml.FXML;
import javafx.scene.control.TabPane;
import javafx.scene.layout.AnchorPane;
@Singleton
public class MainView extends AnchorPane {

	@FXML
	private TabPane mainTabPane;
	
	@Inject
	private MainView(StageManager stageManager) {
		stageManager.loadFXML(this, "mainView.fxml");
	}

	public TabPane getMainTabPane() {
		return mainTabPane;
	}
}
