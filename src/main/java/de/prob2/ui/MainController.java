package de.prob2.ui;

import java.io.IOException;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.internal.UIState;
import javafx.fxml.FXML;


import javafx.fxml.FXMLLoader;
import javafx.scene.control.Accordion;
import javafx.scene.control.Alert;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class MainController extends BorderPane {
	private static final Logger logger = LoggerFactory.getLogger(MainController.class);
	
	private FXMLLoader loader;
	
	@FXML
	private Accordion leftAccordion;
	
	@FXML
	private TitledPane operationsTP;
	
	@FXML
	private TitledPane modelcheckTP;
	
	@FXML
	private TitledPane historyTP;
	
	@FXML
	private TitledPane animationsTP;
	
	@FXML
	private TitledPane statsTP;
	
	
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
		} catch (IOException e) {
			logger.error("loading fxml failed", e);
			Alert alert = new Alert(Alert.AlertType.ERROR, "Could not open file:\n" + e);
			alert.getDialogPane().getStylesheets().add("prob.css");
			alert.showAndWait();
		}	
	}
	
	@FXML
	public void operationsTPClicked() {
		System.out.println(operationsTP.isExpanded());
		System.out.println(operationsTP.getParent());
	}
	
	@FXML
	public void historyTPClicked() {
		
	}
	
	@FXML
	public void modelcheckTPClicked() {
		
	}
	
	@FXML
	public void statsTPClicked() {
		
	}
		
	@FXML
	public void animationsTPClicked() {
			
	}
		
}
