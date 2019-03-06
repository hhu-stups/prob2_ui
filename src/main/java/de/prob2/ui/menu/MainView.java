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
import javafx.scene.control.SplitPane;
import javafx.scene.control.TabPane;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.StackPane;

@FXMLInjected
@Singleton
public class MainView extends StackPane {

	@FXML
	private TabPane tabPane;
	@FXML
	private TitledPane consolePane;
	@FXML
	private SplitPane splitPane;

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
		consolePane.expandedProperty().addListener((observable, from, to) -> splitPane.setDividerPositions(to ? 0.5 : 0.8));
		config.addListener(new ConfigListener() {
			@Override
			public void loadConfig(final ConfigData configData) {
				if (configData.currentMainTab != null) {
					getTabPersistenceHandler().setCurrentTab(configData.currentMainTab);
					consolePane.setExpanded(configData.bConsoleExpanded);
				}
			}
			
			@Override
			public void saveConfig(final ConfigData configData) {
				configData.currentMainTab = getTabPersistenceHandler().getCurrentTab();
				configData.bConsoleExpanded = consolePane.isExpanded();
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
