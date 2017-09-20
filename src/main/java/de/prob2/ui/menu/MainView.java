package de.prob2.ui.menu;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.persistence.TabPersistenceHandler;
import javafx.fxml.FXML;
import javafx.scene.control.TabPane;
import javafx.scene.layout.AnchorPane;

@Singleton
public class MainView extends AnchorPane {
		
	@FXML
	private TabPane tabPane;
		
	private final TabPersistenceHandler tabPersistenceHandler;
	
	@Inject
	private MainView(StageManager stageManager) {
		stageManager.loadFXML(this, "mainView.fxml");
		this.tabPersistenceHandler = new TabPersistenceHandler(tabPane);
	}
	
	public TabPersistenceHandler getTabPersistenceHandler() {
		return tabPersistenceHandler;
	}
	
}
