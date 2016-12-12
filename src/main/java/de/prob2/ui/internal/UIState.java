package de.prob2.ui.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Singleton;


@Singleton
public class UIState {
	
	private static final String[] DETACHED_VALUES = new String[]{"History", "Operations", "Model Check", "Statistics", "Animations"};
	private static final Set<String> DETACHED = new HashSet<>(Arrays.asList(DETACHED_VALUES));
	
	private String guiState;
	
	private HashMap<String, List<Double>> stages;
	
	private List<String> groovyObjectTabs;
		
	@Inject
	public UIState() {
		this.guiState = "main.fxml";
		this.stages = new HashMap<String, List<Double>>();
		this.groovyObjectTabs = new ArrayList<>();
	}
	
	public void setGuiState(String guiState) {
		this.guiState = guiState;
	}
	
	public String getGuiState() {
		return guiState;
	}
	
	public void addStage(String stage, List<Double> stageData) {
		stages.put(stage, stageData);
	}
	
	public void removeStage(String stage) {
		stages.remove(stage);
	}
	
	public HashMap<String, List<Double>> getStages() {
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
		HashMap<String,List<Double>> set = new HashMap<>(stages);
		for(String stage : set.keySet()) {
			if(DETACHED.contains(stage)) {
				stages.remove(stage);
			}
		}
	}

}
