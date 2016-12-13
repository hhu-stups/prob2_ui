package de.prob2.ui.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import javafx.geometry.BoundingBox;


@Singleton
public class UIState {
	private static final Set<String> DETACHED = new HashSet<>(Arrays.asList(new String[] {"History", "Operations", "Model Check", "Statistics", "Animations"}));
	
	private String guiState;
	private HashMap<String, BoundingBox> stages;
	private List<String> groovyObjectTabs;
		
	@Inject
	public UIState() {
		this.guiState = "main.fxml";
		this.stages = new HashMap<>();
		this.groovyObjectTabs = new ArrayList<>();
	}
	
	public void setGuiState(String guiState) {
		this.guiState = guiState;
	}
	
	public String getGuiState() {
		return guiState;
	}
	
	public void addStage(String stage, BoundingBox box) {
		stages.put(stage, box);
	}
	
	public void removeStage(String stage) {
		stages.remove(stage);
	}
	
	public Map<String, BoundingBox> getStages() {
		return stages;
	}
	
	public void addGroovyObjectTab(String tab) {
		groovyObjectTabs.add(tab);
	}
	
	public void addGroovyObjectTab(String tab, int index) {
		groovyObjectTabs.add(index, tab);
	}
	
	public void removeGroovyObjectTab(int index) {
		groovyObjectTabs.remove(index);
	}
		
	public List<String> getGroovyObjectTabs() {
		return groovyObjectTabs;
	}
	
	public void clearDetachedStages() {
		stages.keySet().removeAll(DETACHED);
	}
}
