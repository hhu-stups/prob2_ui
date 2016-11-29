package de.prob2.ui.internal;

import java.util.HashSet;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class UIState {
	
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

}
