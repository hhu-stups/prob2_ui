package de.prob2.ui;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob2.ui.config.Config;
import de.prob2.ui.config.ConfigData;
import de.prob2.ui.config.ConfigListener;
import de.prob2.ui.history.HistoryView;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.PerspectiveKind;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.menu.DetachViewStageController;
import de.prob2.ui.menu.MenuController;
import de.prob2.ui.persistence.UIState;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.ProjectView;
import de.prob2.ui.stats.StatsView;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.value.ObservableIntegerValue;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Accordion;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@FXMLInjected
@Singleton
public class MainController extends BorderPane {
	private static final Logger LOGGER = LoggerFactory.getLogger(MainController.class);

	public static final String DEFAULT_PERSPECTIVE = "main.fxml";

	@FXML private TitledPane historyTP;
	@FXML private HistoryView historyView;
	@FXML private TitledPane statsTP;
	@FXML private StatsView statsView;
	@FXML private TitledPane projectTP;
	@FXML private ProjectView projectView;
	@FXML private SplitPane horizontalSP;
	@FXML private SplitPane verticalSP;
	@FXML private SplitPane verticalSP2;
	@FXML private Accordion centerAccordion1;
	@FXML private ObservableList<Accordion> accordions;
	@FXML private TitledPane visPane;

	private final Injector injector;
	private final StageManager stageManager;
	private final UIState uiState;
	private final I18n i18n;
	private final Config config;
	private DetachViewStageController detacher;

	@Inject
	public MainController(Injector injector, StageManager stageManager, UIState uiState, I18n i18n, Config config) {
		this.injector = injector;
		this.stageManager = stageManager;
		this.uiState = uiState;
		this.i18n = i18n;
		this.config = config;
		this.reloadMainView();
	}

	@FXML
	private void initialize() {
		Platform.runLater(() -> {
			this.detacher = injector.getInstance(DetachViewStageController.class);
			this.getAccordions().stream().forEach(e -> e.getPanes().stream().forEach(this::addContextMenu));
		});
		final ObservableIntegerValue historySize = historyView.getObservableHistorySize();
		final ObservableIntegerValue currentHistoryValue = historyView.getCurrentHistoryPositionProperty();
		this.historyTP.textProperty()
				.bind(i18n.translateBinding("common.views.historyWithState", currentHistoryValue, historySize));

		// Reduce height of bottom accordion if none of its panes are expanded,
		// so that the top view gets more space instead.
		// Otherwise the collapsed accordion takes up about half the space with a useless gray background.
		centerAccordion1.expandedPaneProperty().addListener((o, from, to) -> verticalSP2.setDividerPositions(to == null ? 0.95 : 0.5));
		verticalSP2.setDividerPositions(centerAccordion1.getExpandedPane() == null ? 0.95 : 0.5);

		statsView.lastResultProperty().addListener((o, from, to) -> {
			if (to == null) {
				this.statsTP.setText(i18n.translate("common.views.stats"));
			} else {
				this.statsTP.setText(i18n.translate("common.views.statsWithState",
						to.getNrProcessedNodes(), to.getNrTotalNodes()));
			}
		});

		injector.getInstance(CurrentProject.class).addListener((observable, from, to) -> {
			// When a project is opened, show the project view's machines tab for convenience.
			if (to != null && (from == null || !from.getLocation().equals(to.getLocation()))) {
				projectTP.setExpanded(true);
				projectView.showMachines();
			}
		});

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

				// Restoring horizontal divider positions is disabled for now as it doesn't work reliably - see:
				// https://github.com/hhu-stups/prob-issues/issues/314

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
					.collect(Collectors.toList()); // get ids of all expanded panes
				if (horizontalSP != null) {
					configData.horizontalDividerPositions = horizontalSP.getDividerPositions();
				}
				if (verticalSP != null) {
					configData.verticalDividerPositions = verticalSP.getDividerPositions();
				}
			}
		});
	}

	private void addContextMenu(TitledPane tp) {
		final MenuItem detachItem = new MenuItem(i18n.translate("common.contextMenu.detach"));
		detachItem.setOnAction(event -> {
			detacher.selectForDetach(tp.getContent().getClass().getName());
			detacher.doDetaching();
		});
		ContextMenu menu = new ContextMenu(detachItem);
		// We want to show the detaching context menu only when right-clicking on the title bar,
		// not the actual content of the TitledPane,
		// to avoid conflicting with context menus in the content (e. g. in the visualization views).
		// Unfortunately there's no API for getting the title bar node,
		// so we have to wait for it to appear in the TitledPane's children
		// and then look it up by its CSS class.
		// I don't know how supported this is, but it seems to work.
		tp.getChildrenUnmodifiable().addListener((InvalidationListener)o -> {
			Node tpTitle = tp.lookup(".title");
			if (tpTitle != null) {
				tpTitle.setOnContextMenuRequested(e -> {
					// It's important to use menu.show(tpTitle.getScene().getWindow(), ...) and not just menu.show(tpTitle, ...).
					// For some reason, the second variant leads to scaling issues on macOS - the context menu is rendered at twice the size.
					// (Tested on macOS 13.4 with OpenJFX 17.0.7, on a laptop with a retina/HiDPI internal display and a regular scale external monitor.)
					menu.show(tpTitle.getScene().getWindow(), e.getScreenX(), e.getScreenY());
				});
			}
		});
	}

	public void reloadMainView() {
		final PerspectiveKind kind = this.uiState.getPerspectiveKind();
		final String perspective = this.uiState.getPerspective();
		URL url;
		switch (kind) {
			case PRESET:
				url = this.getClass().getResource(perspective);
				if (url == null) {
					LOGGER.error("Attempted to load nonexistant preset perspective {}", perspective);
					url = null;
				}
				break;

			case CUSTOM:
				try {
					url = new URL(perspective);
				} catch (final MalformedURLException e) {
					LOGGER.error("Custom perspective FXML URL is malformed", e);
					url = null;
				}
				break;

			default:
				throw new AssertionError("Unhandled perspective kind: " + kind);
		}
		if (url == null) {
			LOGGER.error("Failed to load perspective, falling back to default perspective");
			url = this.getClass().getResource(DEFAULT_PERSPECTIVE);
			assert url != null;
			this.uiState.setPerspectiveKind(PerspectiveKind.PRESET);
			this.uiState.setPerspective(DEFAULT_PERSPECTIVE);
		}
		stageManager.loadFXML(this, url);
		injector.getInstance(MenuController.class).setMacMenu();
	}

	public List<Accordion> getAccordions() {
		return Collections.unmodifiableList(accordions);
	}
}
