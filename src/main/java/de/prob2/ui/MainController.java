package de.prob2.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.persistence.UIState;
import javafx.fxml.FXML;
import javafx.scene.control.Accordion;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;

@Singleton
public class MainController extends BorderPane {
	
	private StageManager stageManager;
	
	@FXML
	private Accordion leftAccordion;
	
	@FXML
	private Accordion rightAccordion;

	@FXML
	private Accordion rightAccordion1;

	@FXML
	private Accordion rightAccordion2;
	//If the user creates his own FXML and wants and accordion at the top
	@FXML
	private Accordion topAccordion;

	//If the user creates his own FXML and wants and accordion at the bottom
	@FXML
	private Accordion bottomAccordion;
	
	@FXML
	private TitledPane operationsTP;
	
	@FXML
	private TitledPane verificationsTP;
	
	@FXML
	private TitledPane historyTP;
	
	@FXML
	private TitledPane projectTP;
	
	@FXML
	private TitledPane statsTP;
	
	@FXML
	private SplitPane horizontalSP;
	
	@FXML
	private SplitPane verticalSP;

	@FXML
	private SplitPane verticalSP2;
	
	private final UIState uiState;
	
	@Inject
	public MainController(StageManager stageManager, UIState uiState) {
		this.stageManager = stageManager;
		this.uiState = uiState;
		refresh();
	}
		
	public void refresh() {
		String guiState = "main.fxml";
		if (!uiState.getGuiState().contains("detached")) {
			guiState = uiState.getGuiState();
		}
		stageManager.loadFXML(this, guiState);
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
	public void verificationsTPClicked() {
		handleTitledPaneClicked(verificationsTP);
	}
	
	@FXML
	public void statsTPClicked() {
		handleTitledPaneClicked(statsTP);
	}
	
	@FXML
	public void projectTPClicked() {
		handleTitledPaneClicked(projectTP);
	}
	
	public void handleTitledPaneClicked(TitledPane pane) {
		for (TitledPane titledPane : ((Accordion) pane.getParent()).getPanes()) {
			uiState.getExpandedTitledPanes().remove(titledPane.getText());
		}
		if (pane.isExpanded()) {
			uiState.getExpandedTitledPanes().add(pane.getText());
		} else {
			uiState.getExpandedTitledPanes().remove(pane.getText());
		}
	}
	
	public void expandTitledPane(String titledPane) {
		HashMap<String,TitledPane> titledPanes = getTitledPanesMap();

		if (!titledPanes.containsKey(titledPane)) {
			return;
		}
		for (Accordion accordion : new Accordion[] {leftAccordion, rightAccordion, bottomAccordion, topAccordion}) {
			if (accordion == null) {
				continue;
			}
			if (accordion.getPanes().contains(titledPanes.get(titledPane))) {
				accordion.setExpandedPane(titledPanes.get(titledPane));
			}
		}
	}

	private HashMap<String,TitledPane> getTitledPanesMap() {
		HashMap<String,TitledPane> titledPanes = new HashMap<>();
		titledPanes.put("Operations", operationsTP);
		titledPanes.put("History", historyTP);
		titledPanes.put("Verifications", verificationsTP);
		titledPanes.put("Statistics", statsTP);
		titledPanes.put("Project", projectTP);
		return titledPanes;
	}
	
	public double[] getHorizontalDividerPositions() {
		if (horizontalSP != null) {
			return horizontalSP.getDividerPositions();
		}
		return new double[]{};
	}
	
	public double[] getVerticalDividerPositions() {
		if (verticalSP != null) {
			return verticalSP.getDividerPositions();
		}
		return new double[]{};
	}
	
	public void setHorizontalDividerPositions(double [] pos) {
		if (horizontalSP != null) {
			horizontalSP.setDividerPositions(pos);
		}
	}
	
	public void setVerticalDividerPositions(double[] pos) {
		if (verticalSP != null) {
			verticalSP.setDividerPositions(pos);
		}
	}

	public Map<TitledPane,Accordion> getParentMap() {
		Map<TitledPane,Accordion> parentMap = new HashMap<>();
		for (Accordion accordion : getAccordionList()){
			if (accordion!=null) {
				for (TitledPane pane : getTitledPanesMap().values())
					if (accordion.getPanes().contains(pane))
						parentMap.put(pane,accordion);
			}
		}
		return parentMap;
	}

	private List<Accordion> getAccordionList() {
		List<Accordion> accordionList = new ArrayList<>();
		accordionList.add(leftAccordion);
		accordionList.add(rightAccordion);
		accordionList.add(rightAccordion1);
		accordionList.add(rightAccordion2);
		accordionList.add(topAccordion);
		accordionList.add(bottomAccordion);
		return accordionList;
	}
}
