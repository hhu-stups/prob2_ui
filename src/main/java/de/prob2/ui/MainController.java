package de.prob2.ui;


import java.io.IOException;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.internal.UIState;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.BorderPane;

@Singleton
public class MainController extends BorderPane {
	
	private FXMLLoader loader;
		
	
	@Inject
	public MainController(FXMLLoader loader, UIState uiState) {
		this.loader = loader;
		refresh(uiState);
	}
	
	public void refresh(UIState uiState) {
		String guiState = "main.fxml";
		if(!"detached".equals(uiState.getGuiState())) {
			guiState = uiState.getGuiState();
		}
		loader.setLocation(getClass().getResource(guiState));
		loader.setController(this);
		loader.setRoot(this);
		try {
			loader.load();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	
	
	
}
