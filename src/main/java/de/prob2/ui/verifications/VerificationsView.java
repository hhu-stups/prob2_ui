package de.prob2.ui.verifications;


import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.config.Config;
import de.prob2.ui.config.ConfigData;
import de.prob2.ui.config.ConfigListener;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.persistence.TabPersistenceHandler;

import javafx.fxml.FXML;
import javafx.scene.control.TabPane;
import javafx.scene.layout.AnchorPane;

@FXMLInjected
@Singleton
public class VerificationsView extends AnchorPane {
	
	@FXML
	private TabPane tabPane;
	
	private final Config config;
	
	@Inject
	private VerificationsView(final StageManager stageManager, final Config config) {
		this.config = config;
		stageManager.loadFXML(this, "verificationsView.fxml");
	}
	
	@FXML
	private void initialize() {
		final TabPersistenceHandler tabPersistenceHandler = new TabPersistenceHandler(tabPane);
		config.addListener(new ConfigListener() {
			@Override
			public void loadConfig(final ConfigData configData) {
				if (configData.currentVerificationTab != null) {
					tabPersistenceHandler.setCurrentTab(configData.currentVerificationTab);
				}
			}
			
			@Override
			public void saveConfig(final ConfigData configData) {
				configData.currentVerificationTab = tabPersistenceHandler.getCurrentTab();
			}
		});
	}
}
