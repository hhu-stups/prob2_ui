package de.prob2.ui;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
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
import de.prob2.ui.internal.PerspectiveKind;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.menu.MenuController;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@FXMLInjected
@Singleton
public class MainController extends BorderPane {
	private static final Logger LOGGER = LoggerFactory.getLogger(MainController.class);
	
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
		this.loadMainView(this.uiState.getPerspectiveKind(), this.uiState.getPerspective());
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
				
				if (configData.horizontalDividerPositions != null && horizontalSP != null) {
					horizontalSP.setDividerPositions(configData.horizontalDividerPositions);
				}
				
				if (configData.verticalDividerPositions != null && verticalSP != null) {
					verticalSP.setDividerPositions(configData.verticalDividerPositions);
				}
			}
			
			@Override
			public void saveConfig(final ConfigData configData) {
				configData.expandedTitledPanes = getAccordions().stream()
					.map(Accordion::getExpandedPane)
					.filter(Objects::nonNull)
					.map(Node::getId)
					.collect(Collectors.toList());
				if (horizontalSP != null) {
					configData.horizontalDividerPositions = horizontalSP.getDividerPositions();
				}
				if (verticalSP != null) {
					configData.verticalDividerPositions = verticalSP.getDividerPositions();
				}
			}
		});
	}

	public void loadMainView(final PerspectiveKind kind, final String perspective) {
		URL url;
		switch (kind) {
			case PRESET:
				url = this.getClass().getResource(perspective);
				if (url == null) {
					LOGGER.error("Unknown preset perspective {} found in config, using default perspective", perspective);
					url = this.getClass().getResource("main.fxml");
				}
				break;
			
			case CUSTOM:
				try {
					url = new URL(perspective);
				} catch (final MalformedURLException e) {
					LOGGER.error("Custom perspective FXML URL is malformed, using default perspective", e);
					url = this.getClass().getResource("main.fxml");
				}
				break;
			
			default:
				throw new AssertionError("Unhandled perspective kind: " + kind);
		}
		stageManager.loadFXML(this, url);
		injector.getInstance(MenuController.class).setMacMenu();
	}
	
	public void changeMainView(final PerspectiveKind kind, final String perspective) {
		this.uiState.setPerspectiveKind(kind);
		this.uiState.setPerspective(perspective);
		this.loadMainView(kind, perspective);
	}

	public List<Accordion> getAccordions() {
		return Collections.unmodifiableList(accordions);
	}
}
