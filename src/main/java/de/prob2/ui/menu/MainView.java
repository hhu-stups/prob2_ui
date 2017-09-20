package de.prob2.ui.menu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob2.ui.internal.StageManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.AnchorPane;

@Singleton
public class MainView extends AnchorPane {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(MainView.class);
	
	@FXML
	private TabPane tabPane;
	
	@FXML
	private Tab statesTab;
	
	@FXML
	private Tab statesErrorTab;
	
	@FXML
	private Tab visualisationTab;
	
	private final StringProperty currentTab;
	
	@Inject
	private MainView(StageManager stageManager) {
		this.currentTab = new SimpleStringProperty(this, "currentTab", null);
		stageManager.loadFXML(this, "mainView.fxml");
	}
	
	@FXML
	public void initialize() {
		this.currentTabProperty().addListener((observable, from, to) -> {
			switch (to) {
				case "states":
					this.tabPane.getSelectionModel().select(this.statesTab);
					break;
				
				case "statesError":
					this.tabPane.getSelectionModel().select(this.statesErrorTab);
					break;
					
				case "visualisation":
					this.tabPane.getSelectionModel().select(this.visualisationTab);
					break;
				
				default:
					LOGGER.warn("Attempted to select unknown tab: {}", to);
			}
		});
		this.tabPane.getSelectionModel().selectedItemProperty().addListener((observable, from, to) -> this.setCurrentTab(to.getId()));
		this.setCurrentTab(this.tabPane.getSelectionModel().getSelectedItem().getId());
	}
	
	public StringProperty currentTabProperty() {
		return this.currentTab;
	}
	
	public String getCurrentTab() {
		return this.currentTabProperty().get();
	}
	
	public void setCurrentTab(String tab) {
		this.currentTabProperty().set(tab);
	}

}
