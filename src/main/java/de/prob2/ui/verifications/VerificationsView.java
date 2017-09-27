package de.prob2.ui.verifications;


import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.persistence.TabPersistenceHandler;
import javafx.fxml.FXML;
import javafx.scene.control.TabPane;
import javafx.scene.layout.AnchorPane;

@Singleton
public class VerificationsView extends AnchorPane {
	
	@FXML
	private TabPane tabPane;
	
	private final TabPersistenceHandler tabPersistenceHandler;
			
	@Inject
	private VerificationsView(final StageManager stageManager) {
		stageManager.loadFXML(this, "verificationsView.fxml");
		this.tabPersistenceHandler = new TabPersistenceHandler(tabPane);
	}
	
	public TabPersistenceHandler getTabPersistenceHandler() {
		return tabPersistenceHandler;
	}
	
}
