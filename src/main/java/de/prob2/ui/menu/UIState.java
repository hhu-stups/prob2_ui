package de.prob2.ui.menu;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class UIState {
	
	private String guiState;
	
	@Inject
	public UIState() {
		this.guiState = "main";
	}
	
	public void setGuiState(String guiState) {
		this.guiState = guiState;
	}
	
	public String getGuiState() {
		return guiState;
	}

}
