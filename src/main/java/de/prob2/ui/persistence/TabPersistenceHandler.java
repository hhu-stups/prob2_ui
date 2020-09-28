package de.prob2.ui.persistence;

import java.util.HashMap;

import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

public class TabPersistenceHandler {
	
	private final TabPane pane;
	
	private final HashMap<String, Tab> tabMap;

	public TabPersistenceHandler(TabPane pane) {
		this.pane = pane;
		this.tabMap = new HashMap<>();
		
		for(Tab tab : pane.getTabs()) {
			tabMap.put(tab.getId(), tab);
		}
	}
	
	public String getCurrentTab() {
		return this.pane.getSelectionModel().getSelectedItem().getId();
	}
	
	public void setCurrentTab(String tab) {
		this.pane.getSelectionModel().select(tabMap.get(tab));
	}
}
