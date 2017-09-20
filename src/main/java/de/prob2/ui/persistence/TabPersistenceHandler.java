package de.prob2.ui.persistence;

import java.util.HashMap;


import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

public class TabPersistenceHandler {
	
	private final TabPane pane;
	
	private final StringProperty currentTab;
	
	private final HashMap<String, Tab> tabMap;

	public TabPersistenceHandler(TabPane pane) {
		this.pane = pane;
		this.currentTab = new SimpleStringProperty(this, "currentTab", null);
		this.pane.getSelectionModel().selectedItemProperty().addListener((observable, from, to) -> this.setCurrentTab(to.getId()));
		this.setCurrentTab(this.pane.getSelectionModel().getSelectedItem().getId());
		this.tabMap = new HashMap<>();
		
		for(Tab tab : pane.getTabs()) {
			tabMap.put(tab.getId(), tab);
		}
		
		this.currentTabProperty().addListener((observable, from, to) -> {
			this.pane.getSelectionModel().select(tabMap.get(to));
		});
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
