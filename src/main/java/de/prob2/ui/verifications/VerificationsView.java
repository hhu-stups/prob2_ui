package de.prob2.ui.verifications;

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
public class VerificationsView extends AnchorPane {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(VerificationsView.class);
	
	@FXML
	private TabPane tabPane;
	
	@FXML
	private Tab tabModelchecking;
	
	@FXML
	private Tab tabLTL;
	
	@FXML
	private Tab tabCBC;
	
	private final StringProperty currentTab;
		
	@Inject
	private VerificationsView(final StageManager stageManager) {
		this.currentTab = new SimpleStringProperty(this, "currentTab", null);
		stageManager.loadFXML(this, "verificationsView.fxml");
	}
	
	@FXML
	public void initialize() {
		this.currentTabProperty().addListener((observable, from, to) -> {
			switch (to) {
				case "modelchecking":
					this.tabPane.getSelectionModel().select(this.tabModelchecking);
					break;
				
				case "ltl":
					this.tabPane.getSelectionModel().select(this.tabLTL);
					break;
					
				case "cbc":
					this.tabPane.getSelectionModel().select(this.tabCBC);
					break;
				
				default:
					LOGGER.warn("Attempted to select unknown verification tab: {}", to);
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
