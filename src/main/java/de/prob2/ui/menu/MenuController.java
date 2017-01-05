package de.prob2.ui.menu;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.codecentric.centerdevice.MenuToolkit;

import de.prob.scripting.Api;
import de.prob.scripting.ModelTranslationError;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;

import de.prob2.ui.MainController;
import de.prob2.ui.consoles.b.BConsoleStage;
import de.prob2.ui.consoles.groovy.GroovyConsoleStage;
import de.prob2.ui.formula.FormulaInputStage;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.UIState;
import de.prob2.ui.modelchecking.ModelcheckingController;
import de.prob2.ui.preferences.PreferencesStage;
import de.prob2.ui.prob2fx.CurrentTrace;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class MenuController extends MenuBar {
	public static final boolean IS_MAC = System.getProperty("os.name", "").toLowerCase().contains("mac");
	private static final Logger logger = LoggerFactory.getLogger(MenuController.class);

	private final Injector injector;
	private final Api api;
	private final AnimationSelector animationSelector;
	private final StageManager stageManager;
	private final CurrentTrace currentTrace;
	private final MenuToolkit menuToolkit;
	private final RecentFiles recentFiles;
	private final UIState uiState;
	private final DetachViewStageController dvController;
	
	private final Object openLock;
	private Window window;

	@FXML private Menu recentFilesMenu;
	@FXML private MenuItem recentFilesPlaceholder;
	@FXML private MenuItem clearRecentFiles;
	@FXML private Menu windowMenu;
	@FXML private MenuItem preferencesItem;
	@FXML private MenuItem enterFormulaForVisualization;
	@FXML private MenuItem aboutItem;
	
	@Inject
	private MenuController(
		final StageManager stageManager,
		final Injector injector,
		final Api api,
		final AnimationSelector animationSelector,
		final CurrentTrace currentTrace,
		final DetachViewStageController dvController,
		final RecentFiles recentFiles,
		final UIState uiState
	) {
		this.injector = injector;
		this.api = api;
		this.animationSelector = animationSelector;
		this.stageManager = stageManager;
		this.currentTrace = currentTrace;
		this.dvController = dvController;
		if(IS_MAC) {
			this.menuToolkit = injector.getInstance(MenuToolkit.class);
		} else {
			this.menuToolkit = null;
		}
		this.recentFiles = recentFiles;
		this.uiState = uiState;
		
		this.openLock = new Object();

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
			applicationMenu.getItems().setAll(
				aboutItem,
				new SeparatorMenuItem(),
				preferencesItem,
				new SeparatorMenuItem(),
				menuToolkit.createHideMenuItem("ProB 2"),
				menuToolkit.createHideOthersMenuItem(),
				menuToolkit.createUnhideAllMenuItem(),
				new SeparatorMenuItem(),
				menuToolkit.createQuitMenuItem("ProB 2")
			);
			
			// Add Mac-style items to Window menu
			windowMenu.getItems().addAll(
				menuToolkit.createMinimizeMenuItem(),
				menuToolkit.createZoomMenuItem(),
				menuToolkit.createCycleWindowsItem(),
				new SeparatorMenuItem(),
				menuToolkit.createBringAllToFrontItem(),
				new SeparatorMenuItem()
			);
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
			final List<MenuItem> newItems = getRecentFileItems();

			// If there are no recent files, show a placeholder and disable clearing
			this.clearRecentFiles.setDisable(newItems.isEmpty());
			if (newItems.isEmpty()) {
				newItems.add(this.recentFilesPlaceholder);
			}
			
			// Add a shortcut for reopening the most recent file
			newItems.get(0).setAccelerator(KeyCombination.valueOf("Shift+Shortcut+'O'"));
			
			// Keep the last two items (the separator and the "clear recent files" item)
			newItems.addAll(recentItems.subList(recentItems.size()-2, recentItems.size()));
			
			// Replace the old recents with the new ones
			this.recentFilesMenu.getItems().setAll(newItems);
		};
		this.recentFiles.addListener(recentFilesListener);
		// Fire the listener once to populate the recent files menu
		recentFilesListener.onChanged(null);
		
		this.enterFormulaForVisualization.disableProperty().bind(currentTrace.currentStateProperty().initializedProperty().not());
	}
	
	@FXML
	private void handleClearRecentFiles() {
		this.recentFiles.clear();
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
		loadPreset("stackedLists.fxml");
	}
	
	@FXML
	private void handleLoadDetached() {
		this.dvController.showAndWait();
	}
	
	@FXML
	private void handleLoadPerspective() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Open File");
		fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("FXML Files", "*.fxml"));
		File selectedFile = fileChooser.showOpenDialog(window);
		if (selectedFile != null) {
			try {
				FXMLLoader loader = injector.getInstance(FXMLLoader.class);
				loader.setLocation(selectedFile.toURI().toURL());
				uiState.setGuiState(selectedFile.toString());
				uiState.clearDetachedStages();
				uiState.getExpandedTitledPanes().clear();
				Parent root = loader.load();
				window.getScene().setRoot(root);
			} catch (IOException e) {
				logger.error("loading fxml failed", e);
				stageManager.makeAlert(Alert.AlertType.ERROR, "Could not open file:\n" + e).showAndWait();
			}
		}
	}
	
	private void openAsync(String path) {
		new Thread(() -> this.open(path), "File Opener Thread").start();
	}
	
	private void open(String path) {
		// NOTE: This method may be called from outside the JavaFX main thread, for example from openAsync.
		// This means that all JavaFX calls must be wrapped in Platform.runLater.
		
		// Prevent multiple threads from loading a file at the same time
		synchronized (this.openLock) {
			final StateSpace newSpace;
			try {
				newSpace = this.api.b_load(path);
			} catch (IOException | ModelTranslationError e) {
				logger.error("loading file failed", e);
				Platform.runLater(() -> stageManager.makeAlert(Alert.AlertType.ERROR, "Could not open file:\n" + e).showAndWait());
				return;
			}
			
			this.animationSelector.addNewAnimation(new Trace(newSpace));
			Platform.runLater(() -> {
				injector.getInstance(ModelcheckingController.class).resetView();
				
				// Remove the path first to avoid listing the same file twice.
				this.recentFiles.remove(path);
				this.recentFiles.add(0, path);
			});
		}
	}

	@FXML
	private void handleOpen(ActionEvent event) {
		final FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Open File");
		fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Classical B Files", "*.mch", "*.ref", "*.imp"));

		final File selectedFile = fileChooser.showOpenDialog(this.window);
		if (selectedFile == null) {
			return;
		}

		this.openAsync(selectedFile.getAbsolutePath());
	}

	@FXML
	private void handleClose(final ActionEvent event) {
		final Stage stage = this.stageManager.getCurrent();
		if (stage != null) {
			stage.close();
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

	private List<MenuItem> getRecentFileItems(){
		final List<MenuItem> newItems = new ArrayList<>();
		for (String s : this.recentFiles) {
			final MenuItem item = new MenuItem(new File(s).getName());
			item.setOnAction(event -> this.openAsync(s));
			newItems.add(item);
		}
		return newItems;
	}
}
