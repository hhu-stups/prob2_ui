package de.prob2.ui.menu;


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
public class MainView extends AnchorPane {

	@FXML
	private TabPane tabPane;

	private final Config config;

	private TabPersistenceHandler tabPersistenceHandler;

	@Inject
	private MainView(StageManager stageManager, Config config) {
		this.config = config;

		stageManager.loadFXML(this, "mainView.fxml");
	}

	@FXML
	private void initialize() {
		this.tabPersistenceHandler = new TabPersistenceHandler(tabPane);
		config.addListener(new ConfigListener() {
			@Override
			public void loadConfig(final ConfigData configData) {
				if (configData.currentMainTab != null) {
					getTabPersistenceHandler().setCurrentTab(configData.currentMainTab);
				}
			}
			
			@Override
			public void saveConfig(final ConfigData configData) {
				configData.currentMainTab = getTabPersistenceHandler().getCurrentTab();
			}
		});
	}

	public TabPersistenceHandler getTabPersistenceHandler() {
		return tabPersistenceHandler;
	}

	public TabPane getTabPane() {
		return tabPane;
	}
}
