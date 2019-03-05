package de.prob2.ui;

import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob2.ui.config.Config;
import de.prob2.ui.config.ConfigData;
import de.prob2.ui.config.ConfigListener;
import de.prob2.ui.history.HistoryView;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.persistence.UIState;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.ProjectView;
import de.prob2.ui.stats.StatsView;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableIntegerValue;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Accordion;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;

@FXMLInjected
@Singleton
public class MainController extends BorderPane {
	@FXML private TitledPane historyTP;
	@FXML private HistoryView historyView;
	@FXML private TitledPane statsTP;
	@FXML private StatsView statsView;
	@FXML private TitledPane projectTP;
	@FXML private ProjectView projectView;
	@FXML private SplitPane horizontalSP;
	@FXML private SplitPane verticalSP;
	@FXML private ObservableList<Accordion> accordions;

	private final Injector injector;
	private final StageManager stageManager;
	private final UIState uiState;
	private final ResourceBundle resourceBundle;
	private final Config config;

	@Inject
	public MainController(Injector injector, StageManager stageManager, UIState uiState, ResourceBundle resourceBundle, Config config) {
		this.injector = injector;
		this.stageManager = stageManager;
		this.uiState = uiState;
		this.resourceBundle = resourceBundle;
		this.config = config;
		refresh();
	}

	@FXML
	private void initialize() {
		accordions.forEach(acc -> acc.getPanes().stream().filter(tp -> tp != null && tp.getContent() != null).forEach(tp -> tp.getContent().setVisible(true)));
		final ObservableIntegerValue historySize = historyView.getObservableHistorySize();
		final ObservableValue<Number> currentHistoryValue = historyView.getCurrentHistoryPositionProperty();
		this.historyTP.textProperty()
				.bind(Bindings.format(this.resourceBundle.getString("common.views.historyWithState"), currentHistoryValue, historySize));

		final ObservableIntegerValue currentStatesNumber = statsView.getStatesNumber();
		final ObservableValue<Number> currentProcessedStates = statsView.getProcessedStates();
		this.statsTP.textProperty()
				.bind(Bindings.format(this.resourceBundle.getString("common.views.statsWithState"), currentProcessedStates, currentStatesNumber));
		
		Platform.runLater(() -> injector.getInstance(CurrentProject.class).addListener((observable, from, to) -> {
			if (to != null) {
				projectTP.setExpanded(true);
				projectView.showMachines();
			}
		}));

		config.addListener(new ConfigListener() {
			@Override
			public void loadConfig(final ConfigData configData) {
				if (configData.expandedTitledPanes != null) {
					getAccordions().forEach(acc ->
						acc.getPanes().stream()
							.filter(tp -> configData.expandedTitledPanes.contains(tp.getId()))
							.forEach(acc::setExpandedPane)
					);
				}
				
				if (configData.horizontalDividerPositions != null) {
					horizontalSP.setDividerPositions(configData.horizontalDividerPositions);
				}
				
				if (configData.verticalDividerPositions != null) {
					verticalSP.setDividerPositions(configData.verticalDividerPositions);
				}
			}
			
			@Override
			public void saveConfig(final ConfigData configData) {
				configData.expandedTitledPanes = getAccordions().stream()
					.map(Accordion::getExpandedPane)
					.map(Node::getId)
					.collect(Collectors.toList());
				configData.horizontalDividerPositions = horizontalSP.getDividerPositions();
				configData.verticalDividerPositions = verticalSP.getDividerPositions();
			}
		});
	}

	public void refresh() {
		String guiState = "main.fxml";
		if (!uiState.getGuiState().contains("detached")) {
			guiState = uiState.getGuiState();
		}
		stageManager.loadFXML(this, guiState);
	}

	public List<Accordion> getAccordions() {
		return Collections.unmodifiableList(accordions);
	}
}
