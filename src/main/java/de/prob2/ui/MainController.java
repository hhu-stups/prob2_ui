package de.prob2.ui;

import java.io.IOException;
import java.util.HashMap;

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
	
	public enum TitledPaneExpanded {
		OPERATIONS, HISTORY, ANIMATIONS, MODELCHECK, STATS;
	}
	
	private static final Logger logger = LoggerFactory.getLogger(MainController.class);
	
	private FXMLLoader loader;
	
	@FXML
	private Accordion leftAccordion;
	
	@FXML
	private Accordion rightAccordion;
	
	//If the user creates his own FXML and wants and accordion at the top
	@FXML
	private Accordion topAccordion;
	
	//If the user creates his own FXML and wants and accordion at the bottom
	@FXML
	private Accordion bottomAccordion;
	
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
	
	private final UIState uiState;
		
	
	@Inject
	public MainController(FXMLLoader loader, UIState uiState) {
		this.loader = loader;
		this.uiState = uiState;
		refresh();
	}
	
	public void refresh() {
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
		handleTitledPaneClicked(operationsTP);
	}
	
	@FXML
	public void historyTPClicked() {
		handleTitledPaneClicked(historyTP);
	}
	
	@FXML
	public void modelcheckTPClicked() {
		handleTitledPaneClicked(modelcheckTP);
	}
	
	@FXML
	public void statsTPClicked() {
		handleTitledPaneClicked(statsTP);
	}
		
	@FXML
	public void animationsTPClicked() {
		handleTitledPaneClicked(animationsTP);	
	}
	
	public void handleTitledPaneClicked(TitledPane pane) {
		for (TitledPane titledPane : ((Accordion) pane.getParent()).getPanes()) {
			uiState.getExpandedTitledPanes().remove(titledPane.getText());
		}
		if(pane.isExpanded()) {
			uiState.getExpandedTitledPanes().add(pane.getText());
		} else {
			uiState.getExpandedTitledPanes().remove(pane.getText());
		}
	}
	
	public void expandTitledPane(String titledPane) {
		Accordion[] accordions = new Accordion[] {leftAccordion, rightAccordion, bottomAccordion, topAccordion};

 		HashMap<String,TitledPane> titledPanes = new HashMap<String, TitledPane>();
 		titledPanes.put("Operations", operationsTP);
 		titledPanes.put("History", historyTP);
 		titledPanes.put("Animations", animationsTP);
 		titledPanes.put("Model Check", modelcheckTP);
 		titledPanes.put("Statistics", statsTP);

 		if(titledPanes.get(titledPane) == null) {
 			return;
 		}
		for (Accordion accordion : accordions) {
			if(accordion == null) {
				continue;
			}
			if(accordion.getPanes().contains(titledPanes.get(titledPane))) {
				accordion.setExpandedPane(titledPanes.get(titledPane));
			}			
		}

	}
		
}
