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
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.StackPane;

import java.util.List;

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

	@Inject
	private MainView(StageManager stageManager, Config config) {
		this.config = config;

		stageManager.loadFXML(this, "mainView.fxml");
	}

	@FXML
	private void initialize() {
		consolePane.expandedProperty().addListener((observable, from, to) -> splitPane.setDividerPositions(to ? 0.5 : 0.8));
		config.addListener(new ConfigListener() {
			@Override
			public void loadConfig(final ConfigData configData) {
				if (configData.currentMainTab != null) {
					TabPersistenceHandler.setCurrentTab(tabPane, configData.currentMainTab);
					consolePane.setExpanded(configData.bConsoleExpanded);
				}
			}
			
			@Override
			public void saveConfig(final ConfigData configData) {
				configData.currentMainTab = TabPersistenceHandler.getCurrentTab(tabPane);
				configData.bConsoleExpanded = consolePane.isExpanded();
			}
		});
	}

	public TabPane getTabPane() {
		return tabPane;
	}

	public void switchTabPane(String id) {
		List<Tab> tabs = tabPane.getTabs();
		for(Tab tab : tabs) {
			if(tab.getId().equals(id)) {
				tabPane.getSelectionModel().select(tab);
				break;
			}
		}
	}
}
