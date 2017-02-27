package de.prob2.ui.menu;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.codecentric.centerdevice.MenuToolkit;
import de.prob.scripting.ModelTranslationError;
import de.prob2.ui.MainController;
import de.prob2.ui.consoles.b.BConsoleStage;
import de.prob2.ui.consoles.groovy.GroovyConsoleStage;
import de.prob2.ui.formula.FormulaInputStage;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.modelchecking.ModelcheckingController;
import de.prob2.ui.persistence.UIState;
import de.prob2.ui.preferences.PreferencesStage;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.Machine;
import de.prob2.ui.project.MachineLoader;
import de.prob2.ui.project.NewProjectStage;
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
import javafx.scene.control.SplitPane;
import javafx.scene.input.KeyCombination;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;

@Singleton
public final class MenuController extends MenuBar {
	private static final Logger logger = LoggerFactory.getLogger(MenuController.class);

	private final Injector injector;
	private final StageManager stageManager;
	private final CurrentTrace currentTrace;
	private final MenuToolkit menuToolkit;
	private final RecentFiles recentFiles;
	private final UIState uiState;
	private final DetachViewStageController dvController;
	private final AboutBoxController aboutController;
	private Window window;

	@FXML
	private Menu recentFilesMenu;
	@FXML
	private MenuItem recentFilesPlaceholder;
	@FXML
	private MenuItem clearRecentFiles;
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
	private MachineLoader machineLoader;

	@Inject
	private MenuController(final StageManager stageManager, final Injector injector, final CurrentTrace currentTrace,
			final DetachViewStageController dvController, final AboutBoxController aboutController,
			@Nullable final MenuToolkit menuToolkit, final RecentFiles recentFiles, final CurrentProject currentProject,
			final UIState uiState, final MachineLoader machineLoader) {
		this.injector = injector;
		this.stageManager = stageManager;
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.dvController = dvController;
		this.aboutController = aboutController;
		this.menuToolkit = menuToolkit;
		this.recentFiles = recentFiles;
		this.uiState = uiState;
		this.machineLoader = machineLoader;

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
			final Menu applicationMenu = menuToolkit.createDefaultApplicationMenu("ProB 2");
			this.getMenus().add(0, applicationMenu);

			menuToolkit.setApplicationMenu(applicationMenu);
			applicationMenu.getItems().setAll(aboutItem, new SeparatorMenuItem(), preferencesItem,
					new SeparatorMenuItem(), menuToolkit.createHideMenuItem("ProB 2"),
					menuToolkit.createHideOthersMenuItem(), menuToolkit.createUnhideAllMenuItem(),
					new SeparatorMenuItem(), menuToolkit.createQuitMenuItem("ProB 2"));

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

		final ListChangeListener<String> recentFilesListener = change -> {
			final ObservableList<MenuItem> recentItems = this.recentFilesMenu.getItems();
			final List<MenuItem> newItems = getRecentFileItems(recentFiles.recentFilesProperty(), "files");

			// If there are no recent files, show a placeholder and disable
			// clearing
			this.clearRecentFiles.setDisable(newItems.isEmpty());
			if (newItems.isEmpty()) {
				newItems.add(this.recentFilesPlaceholder);
			}

			// Add a shortcut for reopening the most recent file
			newItems.get(0).setAccelerator(KeyCombination.valueOf("Shift+Shortcut+'O'"));

			// Keep the last two items (the separator and the "clear recent
			// files" item)
			newItems.addAll(recentItems.subList(recentItems.size() - 2, recentItems.size()));

			// Replace the old recents with the new ones
			this.recentFilesMenu.getItems().setAll(newItems);
		};
		this.recentFiles.recentFilesProperty().addListener(recentFilesListener);
		// Fire the listener once to populate the recent files menu
		recentFilesListener.onChanged(null);

		final ListChangeListener<String> recentProjectsListener = change -> {
			final ObservableList<MenuItem> recentItems = this.recentProjectsMenu.getItems();
			final List<MenuItem> newItems = getRecentFileItems(recentFiles.recentProjectsProperty(), "projects");
			this.clearRecentProjects.setDisable(newItems.isEmpty());
			if (newItems.isEmpty()) {
				newItems.add(this.recentProjectsPlaceholder);
			}
			newItems.addAll(recentItems.subList(recentItems.size() - 2, recentItems.size()));
			this.recentProjectsMenu.getItems().setAll(newItems);
		};
		this.recentFiles.recentProjectsProperty().addListener(recentProjectsListener);
		recentProjectsListener.onChanged(null);

		this.saveProjectItem.disableProperty().bind(currentProject.existsProperty().not());
		this.reloadMachineItem.disableProperty().bind(currentTrace.existsProperty().not());
		this.enterFormulaForVisualization.disableProperty()
				.bind(currentTrace.currentStateProperty().initializedProperty().not());
	}

	@FXML
	private void handleClearRecentFiles() {
		this.recentFiles.recentFilesProperty().clear();
	}

	@FXML
	private void handleClearRecentProjects() {
		this.recentFiles.recentProjectsProperty().clear();
	}

	@FXML
	private void handleLoadDefault() {
		uiState.clearDetachedStages();
		uiState.getExpandedTitledPanes().clear();
		loadPreset("main.fxml");
	}

	@FXML
	private void handleLoadSeparated() {
		uiState.clearDetachedStages();
		uiState.getExpandedTitledPanes().clear();
		loadPreset("separatedHistory.fxml");
	}

	@FXML
	private void handleLoadSeparated2() {
		uiState.clearDetachedStages();
		uiState.getExpandedTitledPanes().clear();
		loadPreset("separatedHistoryAndStatistics.fxml");
	}

	@FXML
	private void handleLoadStacked() {
		uiState.clearDetachedStages();
		uiState.getExpandedTitledPanes().clear();
		Parent root = loadPreset("stackedLists.fxml");
//		SplitPane main = (SplitPane) root.getChildrenUnmodifiable().get(1);
//		SplitPane vertical = (SplitPane) main.getItems().get(1);
//		vertical.getItems().get(1).setVisible(true);
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
				uiState.clearDetachedStages();
				uiState.getExpandedTitledPanes().clear();
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

	@FXML
	private void handleOpen() {
		final FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Open File");
		fileChooser.getExtensionFilters()
				.add(new FileChooser.ExtensionFilter("Classical B Files", "*.mch", "*.ref", "*.imp"));

		final File selectedFile = fileChooser.showOpenDialog(this.window);
		if (selectedFile == null) {
			return;
		}

		this.open(selectedFile);
	}

	private void open(File file) {
		machineLoader.loadAsync(new Machine(file.getName().split("\\.")[0], "", file.toPath()));
		Platform.runLater(() -> {
			injector.getInstance(ModelcheckingController.class).resetView();

			// Remove the path first to avoid listing the same file twice.
			this.recentFiles.recentFilesProperty().remove(file.getAbsolutePath());
			this.recentFiles.recentFilesProperty().add(0, file.getAbsolutePath());
		});
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
			this.recentFiles.recentProjectsProperty().remove(file.getAbsolutePath());
			this.recentFiles.recentProjectsProperty().add(0, file.getAbsolutePath());
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
			stage.close();
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

	private List<MenuItem> getRecentFileItems(SimpleListProperty<String> recentListProperty, String filesOrProjects) {
		final List<MenuItem> newItems = new ArrayList<>();
		for (String s : recentListProperty) {
			File file = new File(s);
			final MenuItem item = new MenuItem(file.getName());
			if ("projects".equals(filesOrProjects)) {
				item.setOnAction(event -> this.openProject(file));
			} else if ("files".equals(filesOrProjects)) {
				item.setOnAction(event -> this.open(file));
			}
			newItems.add(item);
		}
		return newItems;
	}
}
