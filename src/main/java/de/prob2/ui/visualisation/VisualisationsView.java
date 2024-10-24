package de.prob2.ui.visualisation;


import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.config.Config;
import de.prob2.ui.config.ConfigData;
import de.prob2.ui.config.ConfigListener;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.persistence.PersistenceUtils;

import javafx.fxml.FXML;
import javafx.scene.control.TabPane;
import javafx.scene.layout.StackPane;

@FXMLInjected
@Singleton
public final class VisualisationsView extends StackPane {
	@FXML
	private TabPane tabPane;

	private final Config config;

	@Inject
	private VisualisationsView(final StageManager stageManager, final Config config) {
		this.config = config;
		stageManager.loadFXML(this, "visualisationsView.fxml");
	}

	@FXML
	private void initialize() {
		config.addListener(new ConfigListener() {
			@Override
			public void loadConfig(final ConfigData configData) {
				if (configData.currentVisualisationTab != null) {
					PersistenceUtils.setCurrentTab(tabPane, configData.currentVisualisationTab);
				}
			}

			@Override
			public void saveConfig(final ConfigData configData) {
				configData.currentVisualisationTab = PersistenceUtils.getCurrentTab(tabPane);
			}
		});
	}
}
