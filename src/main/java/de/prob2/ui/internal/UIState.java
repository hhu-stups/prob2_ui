package de.prob2.ui.internal;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Singleton;


@Singleton
public class UIState {
	
	private static final String[] DETACHED_VALUES = new String[]{"History", "Operations", "Model Check", "Statistics", "Animations"};
	private static final Set<String> DETACHED = new HashSet<>(Arrays.asList(DETACHED_VALUES));
	
	private String guiState;
	
	private Set<String> stages;
	
	@Inject
	public UIState() {
		this.guiState = "main.fxml";
		this.stages = new HashSet<>();
	}
	
	public void setGuiState(String guiState) {
		this.guiState = guiState;
	}
	
	public String getGuiState() {
		return guiState;
	}
	
	public void addStage(String stage) {
		stages.add(stage);
	}
	
	public void removeStage(String stage) {
		stages.remove(stage);
	}
	
	public Set<String> getStages() {
		return stages;
	}
	
	public void clearDetachedStages() {
		HashSet<String> set = new HashSet<>(stages);
		for(String stage : set) {
			if(DETACHED.contains(stage)) {
				stages.remove(stage);
			}
		}
	}

}
