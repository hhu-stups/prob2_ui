package de.prob2.ui.menu;

import java.util.ArrayList;
import java.util.List;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class UIState {
	
	private String guiState;
	
	private List<String> detachedViews;
	
	@Inject
	public UIState() {
		this.guiState = "main.fxml";
		this.detachedViews = new ArrayList<>();
	}
	
	public void setGuiState(String guiState) {
		this.guiState = guiState;
	}
	
	public String getGuiState() {
		return guiState;
	}
	
	public void addView(String view) {
		detachedViews.add(view);
	}
	
	public void removeView(String view) {
		detachedViews.remove(view);
	}
	
	public List<String> getDetachedViews() {
		return detachedViews;
	}

}
