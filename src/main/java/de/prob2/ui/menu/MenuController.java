package de.prob2.ui.menu;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.codecentric.centerdevice.MenuToolkit;

import de.prob.scripting.ModelTranslationError;

import de.prob2.ui.MainController;
import de.prob2.ui.chart.HistoryChartStage;
import de.prob2.ui.consoles.b.BConsoleStage;
import de.prob2.ui.consoles.groovy.GroovyConsoleStage;
import de.prob2.ui.formula.FormulaInputStage;
import de.prob2.ui.history.HistoryView;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.layout.FontSize;
import de.prob2.ui.operations.OperationsView;
import de.prob2.ui.persistence.UIState;
import de.prob2.ui.preferences.PreferencesStage;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.NewProjectStage;
import de.prob2.ui.project.Project;
import de.prob2.ui.project.ProjectView;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.project.runconfigurations.Runconfiguration;
import de.prob2.ui.stats.StatsView;
import de.prob2.ui.verifications.VerificationsView;
import de.prob2.ui.verifications.modelchecking.ModelcheckingController;

import javafx.application.Platform;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.KeyCombination;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class MenuController extends MenuBar {
	private static final Logger logger = LoggerFactory.getLogger(MenuController.class);

	private final Injector injector;
	private final StageManager stageManager;
	private final CurrentTrace currentTrace;
	private final MenuToolkit menuToolkit;
	private final RecentProjects recentProjects;
	private final UIState uiState;
	private final DetachViewStageController dvController;
	private final AboutBoxController aboutController;
	private Window window;

	@FXML
	private Menu recentProjectsMenu;
	@FXML
	private MenuItem recentProjectsPlaceholder;
	@FXML
	private MenuItem clearRecentProjects;
	@FXML
	private Menu windowMenu;
	@FXML
	private MenuItem saveProjectItem;
	@FXML
	private MenuItem reloadMachineItem;
	@FXML
	private MenuItem preferencesItem;
	@FXML
	private MenuItem enterFormulaForVisualization;
	@FXML
	private MenuItem aboutItem;

	private CurrentProject currentProject;
	private static final String PROB2 = "ProB 2";

	@Inject
	private MenuController(final StageManager stageManager, final Injector injector, final CurrentTrace currentTrace,
			final DetachViewStageController dvController, final AboutBoxController aboutController,
			@Nullable final MenuToolkit menuToolkit, final RecentProjects recentProjects,
			final CurrentProject currentProject, final UIState uiState) {
		this.injector = injector;
		this.stageManager = stageManager;
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.dvController = dvController;
		this.aboutController = aboutController;
		this.menuToolkit = menuToolkit;
		this.recentProjects = recentProjects;
		this.uiState = uiState;
		stageManager.loadFXML(this, "menu.fxml");

		if (menuToolkit != null) {
			// Mac-specific menu stuff
			this.setUseSystemMenuBar(true);

			// Remove About menu item from Help
			aboutItem.getParentMenu().getItems().remove(aboutItem);
			aboutItem.setText("About ProB 2");

			// Remove Preferences menu item from Edit
			preferencesItem.getParentMenu().getItems().remove(preferencesItem);
			preferencesItem.setAccelerator(KeyCombination.valueOf("Shortcut+,"));

			// Create Mac-style application menu
			final Menu applicationMenu = menuToolkit.createDefaultApplicationMenu(PROB2);
			this.getMenus().add(0, applicationMenu);

			menuToolkit.setApplicationMenu(applicationMenu);
			MenuItem quit = menuToolkit.createQuitMenuItem(PROB2);
			quit.setOnAction(event -> {
				for (Stage stage : stageManager.getRegistered()) {
					stage.fireEvent(new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST));
				}
			});
			applicationMenu.getItems().setAll(aboutItem, new SeparatorMenuItem(), preferencesItem,
					new SeparatorMenuItem(), menuToolkit.createHideMenuItem(PROB2),
					menuToolkit.createHideOthersMenuItem(), menuToolkit.createUnhideAllMenuItem(),
					new SeparatorMenuItem(), quit);

			// Add Mac-style items to Window menu
			windowMenu.getItems().addAll(menuToolkit.createMinimizeMenuItem(), menuToolkit.createZoomMenuItem(),
					menuToolkit.createCycleWindowsItem(), new SeparatorMenuItem(),
					menuToolkit.createBringAllToFrontItem(), new SeparatorMenuItem());
			menuToolkit.autoAddWindowMenuItems(windowMenu);

			// Make this the global menu bar
			stageManager.setGlobalMacMenuBar(this);
		}
	}

	@FXML
	public void initialize() {
		this.sceneProperty().addListener((observable, from, to) -> {
			if (to != null) {
				to.windowProperty().addListener((observable1, from1, to1) -> this.window = to1);
			}
		});

		final ListChangeListener<String> recentProjectsListener = change -> {
			final ObservableList<MenuItem> recentItems = this.recentProjectsMenu.getItems();
			final List<MenuItem> newItems = getRecentProjectItems(recentProjects);
			this.clearRecentProjects.setDisable(newItems.isEmpty());
			if (newItems.isEmpty()) {
				newItems.add(this.recentProjectsPlaceholder);
			}
			newItems.addAll(recentItems.subList(recentItems.size() - 2, recentItems.size()));
			this.recentProjectsMenu.getItems().setAll(newItems);
		};
		this.recentProjects.addListener(recentProjectsListener);
		recentProjectsListener.onChanged(null);

		this.saveProjectItem.disableProperty().bind(currentProject.existsProperty().not());
		this.reloadMachineItem.disableProperty().bind(currentTrace.existsProperty().not());
		this.enterFormulaForVisualization.disableProperty()
				.bind(currentTrace.currentStateProperty().initializedProperty().not());
	}

	@FXML
	private void handleClearRecentProjects() {
		this.recentProjects.clear();
	}

	@FXML
	private void handleLoadDefault() {
		reset();
		loadPreset("main.fxml");
	}

	@FXML
	private void handleLoadSeparated() {
		reset();
		loadPreset("separatedHistory.fxml");
	}

	@FXML
	private void handleLoadSeparated2() {
		reset();
		loadPreset("separatedHistoryAndStatistics.fxml");
	}

	@FXML
	private void handleLoadDetached() {
		this.dvController.showAndWait();
	}

	@FXML
	private void handleAboutDialog() {
		this.aboutController.showAndWait();
	}

	@FXML
	private void handleLoadPerspective() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Open File");
		fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("FXML Files", "*.fxml"));
		File selectedFile = fileChooser.showOpenDialog(window);
		if (selectedFile != null) {
			try {
				MainController main = injector.getInstance(MainController.class);
				FXMLLoader loader = injector.getInstance(FXMLLoader.class);
				loader.setLocation(selectedFile.toURI().toURL());
				uiState.setGuiState("custom " + selectedFile.toURI().toURL().toExternalForm());
				reset();
				loader.setRoot(main);
				loader.setController(main);
				Parent root = loader.load();
				window.getScene().setRoot(root);
			} catch (IOException e) {
				logger.error("Loading fxml failed", e);
				stageManager.makeAlert(Alert.AlertType.ERROR, "Could not open file:\n" + e).showAndWait();
			}
		}
	}

	private void reset() {
		uiState.clearDetachedStages();
		uiState.getExpandedTitledPanes().clear();
		dvController.resetCheckboxes();
		injector.getInstance(OperationsView.class).setVisible(true);
		injector.getInstance(HistoryView.class).setVisible(true);
		injector.getInstance(StatsView.class).setVisible(true);
		injector.getInstance(VerificationsView.class).setVisible(true);
		injector.getInstance(ProjectView.class).setVisible(true);
	}

	@FXML
	private void handleNewProjectFromFile() {
		final FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Open File");
		fileChooser.getExtensionFilters()
				.add(new FileChooser.ExtensionFilter("Classical B Files", "*.mch", "*.ref", "*.imp"));

		final File selectedFile = fileChooser.showOpenDialog(this.window);
		if (selectedFile == null) {
			return;
		}
		Path projectLocation = currentProject.getDefaultLocation();
		Path absolute = selectedFile.toPath();
		Path relative = projectLocation.relativize(absolute);
		Machine machine = new Machine(selectedFile.getName().substring(0, selectedFile.getName().lastIndexOf('.')), "",
				relative);
		currentProject.set(new Project(selectedFile.getName().substring(0, selectedFile.getName().lastIndexOf('.')),
				"(this project was created automatically from file " + selectedFile.getAbsolutePath() + ")", machine,
				currentProject.getDefaultLocation().toFile()));
		Runconfiguration defaultRunconfig = new Runconfiguration(machine.getName(), "default");
		currentProject.addRunconfiguration(defaultRunconfig);

		currentProject.startAnimation(defaultRunconfig);
	}

	@FXML
	private void handleOpenProject() {
		final FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Open Project");
		fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("ProB2 Projects", "*.json"));

		final File selectedProject = fileChooser.showOpenDialog(this.window);
		if (selectedProject == null) {
			return;
		}

		this.openProject(selectedProject);
	}

	private void openProject(File file) {
		currentProject.open(file);

		Platform.runLater(() -> {
			injector.getInstance(ModelcheckingController.class).resetView();
			this.recentProjects.remove(file.getAbsolutePath());
			this.recentProjects.add(0, file.getAbsolutePath());
		});
	}

	@FXML
	private void createNewProject() {
		final Stage newProjectStage = injector.getInstance(NewProjectStage.class);
		newProjectStage.showAndWait();
		newProjectStage.toFront();
	}

	@FXML
	private void saveProject() {
		currentProject.save();
	}

	@FXML
	private void handleClose() {
		final Stage stage = this.stageManager.getCurrent();
		if (stage != null) {
			stage.fireEvent(new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST));
		}
	}

	@FXML
	private void handleReloadMachine() {
		try {
			this.currentTrace.reload(this.currentTrace.get());
		} catch (IOException | ModelTranslationError e) {
			logger.error("Model reload failed", e);
			stageManager.makeAlert(Alert.AlertType.ERROR, "Failed to reload model:\n" + e).showAndWait();
		}
	}

	@FXML
	private void handlePreferences() {
		final Stage preferencesStage = injector.getInstance(PreferencesStage.class);
		preferencesStage.show();
		preferencesStage.toFront();
	}

	@FXML
	private void handleFormulaInput() {
		final Stage formulaInputStage = injector.getInstance(FormulaInputStage.class);
		formulaInputStage.showAndWait();
		formulaInputStage.toFront();
	}

	@FXML
	private void handleHistoryChart() {
		final Stage chartStage = injector.getInstance(HistoryChartStage.class);
		chartStage.show();
		chartStage.toFront();
	}

	@FXML
	private void handleGroovyConsole() {
		final Stage groovyConsoleStage = injector.getInstance(GroovyConsoleStage.class);
		groovyConsoleStage.show();
		groovyConsoleStage.toFront();
	}

	@FXML
	private void handleBConsole() {
		final Stage bConsoleStage = injector.getInstance(BConsoleStage.class);
		bConsoleStage.show();
		bConsoleStage.toFront();
	}

	@FXML
	private void handleDefaultFontSize() {
		FontSize fontSize = injector.getInstance(FontSize.class);
		fontSize.set(13);
	}

	@FXML
	private void handleIncreaseFontSize() {
		FontSize fontSize = injector.getInstance(FontSize.class);
		fontSize.set(fontSize.get() + 1);
	}

	@FXML
	private void handleDecreaseFontSize() {
		FontSize fontSize = injector.getInstance(FontSize.class);
		fontSize.set(fontSize.get() - 1);
	}

	@FXML
	private void handleReportBug() {
		final Stage reportBugStage = injector.getInstance(ReportBugStage.class);
		reportBugStage.show();
		reportBugStage.toFront();
	}

	public Parent loadPreset(String location) {
		this.uiState.setGuiState(location);
		final MainController root = injector.getInstance(MainController.class);
		root.refresh();
		window.getScene().setRoot(root);

		if (this.menuToolkit != null) {
			this.menuToolkit.setApplicationMenu(this.getMenus().get(0));
			this.stageManager.setGlobalMacMenuBar(this);
		}
		return root;
	}

	private List<MenuItem> getRecentProjectItems(SimpleListProperty<String> recentListProperty) {
		final List<MenuItem> newItems = new ArrayList<>();
		for (String s : recentListProperty) {
			File file = new File(s);
			final MenuItem item = new MenuItem(file.getName());
			item.setOnAction(event -> this.openProject(file));
			newItems.add(item);
		}
		return newItems;
	}
}
