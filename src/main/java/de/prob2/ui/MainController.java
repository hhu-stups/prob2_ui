package de.prob2.ui;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import de.prob2.ui.history.HistoryView;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.persistence.UIState;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableIntegerValue;
import javafx.fxml.FXML;
import javafx.scene.control.Accordion;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;

import java.util.*;

@Singleton
public class MainController extends BorderPane {
	@FXML
	private Accordion leftAccordion;

	@FXML
	private Accordion leftAccordion1;

	@FXML
	private Accordion leftAccordion2;
	
	@FXML
	private Accordion rightAccordion;

	@FXML
	private Accordion rightAccordion1;

	@FXML
	private Accordion rightAccordion2;

	//If the user creates his own FXML and wants and accordion at the top
	@FXML
	private Accordion topAccordion;

	@FXML
	private Accordion topAccordion1;

	@FXML
	private Accordion topAccordion2;

	@FXML
	private Accordion topAccordion3;

	//If the user creates his own FXML and wants and accordion at the bottom
	@FXML
	private Accordion bottomAccordion;

	@FXML
	private Accordion bottomAccordion1;

	@FXML
	private Accordion bottomAccordion2;

	@FXML
	private Accordion bottomAccordion3;
	
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
	
	private final Injector injector;
	private final StageManager stageManager;
	private final UIState uiState;
	private final ResourceBundle resourceBundle;
	
	@Inject
	public MainController(Injector injector, StageManager stageManager, UIState uiState, ResourceBundle resourceBundle) {
		this.injector = injector;
		this.stageManager = stageManager;
		this.uiState = uiState;
		this.resourceBundle = resourceBundle;
		refresh();
	}
			
	public void refresh() {
		String guiState = "main.fxml";
		if (!uiState.getGuiState().contains("detached")) {
			guiState = uiState.getGuiState();
		}
		getTitledPanesMap().values().stream().filter(tp -> tp != null && tp.getContent() != null).forEach(tp -> tp.getContent().setVisible(true));
		stageManager.loadFXML(this, guiState);
	}
	
	@FXML
	private void initialize() {
		final ObservableIntegerValue size = this.injector.getInstance(HistoryView.class).getObservableHistorySize();
		this.historyTP.textProperty().bind(Bindings.format(this.resourceBundle.getString("tptitles.historyWithSize"), size));
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
		for (Accordion accordion : getAccordionList()) {
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

	public Map<TitledPane,Accordion> getAccordionMap() {
		Map<TitledPane,Accordion> parentMap = new HashMap<>();
		getAccordionList().stream().filter(Objects::nonNull).forEach(accordion ->
			getTitledPanesMap().values().stream().filter(pane -> accordion.getPanes().contains(pane)).forEach(pane -> parentMap.put(pane, accordion))
		);
		return parentMap;
	}

	private List<Accordion> getAccordionList() {
		List<Accordion> accordionList = new ArrayList<>();
		accordionList.add(leftAccordion);
		accordionList.add(leftAccordion1);
		accordionList.add(leftAccordion2);
		accordionList.add(rightAccordion);
		accordionList.add(rightAccordion1);
		accordionList.add(rightAccordion2);
		accordionList.add(topAccordion);
		accordionList.add(topAccordion1);
		accordionList.add(topAccordion2);
		accordionList.add(topAccordion3);
		accordionList.add(bottomAccordion);
		accordionList.add(bottomAccordion1);
		accordionList.add(bottomAccordion2);
		accordionList.add(bottomAccordion3);
		return accordionList;
	}
}
