package de.prob2.ui.menu;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob2.ui.config.Config;
import de.prob2.ui.config.ConfigData;
import de.prob2.ui.config.ConfigListener;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.persistence.PersistenceUtils;
import javafx.fxml.FXML;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

import javafx.scene.layout.StackPane;

import java.util.List;

@FXMLInjected
@Singleton
public class MainView extends StackPane {

	@FXML
	private TabPane tabPane;
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
		config.addListener(new ConfigListener() {
			@Override
			public void loadConfig(final ConfigData configData) {
				if (configData.currentMainTab != null) {
					PersistenceUtils.setCurrentTab(tabPane, configData.currentMainTab);
				}
			}
			
			@Override
			public void saveConfig(final ConfigData configData) {
				configData.currentMainTab = PersistenceUtils.getCurrentTab(tabPane);
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
