package de.prob2.ui.menu;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import de.prob2.ui.animations.AnimationsView;
import de.prob2.ui.history.HistoryView;
import de.prob2.ui.operations.OperationsView;
import de.prob2.ui.stats.StatsView;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.be4.classicalb.core.parser.exceptions.BException;
import de.codecentric.centerdevice.MenuToolkit;
import de.prob.scripting.Api;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import de.prob2.ui.consoles.b.BConsoleStage;
import de.prob2.ui.consoles.groovy.GroovyConsoleStage;
import de.prob2.ui.dotty.DottyStage;
import de.prob2.ui.formula.FormulaInputStage;
import de.prob2.ui.modelchecking.ModelcheckingController;
import de.prob2.ui.preferences.PreferencesStage;
import de.prob2.ui.prob2fx.CurrentStage;
import de.prob2.ui.prob2fx.CurrentTrace;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCombination;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;

@Singleton
public final class MenuController extends MenuBar {
	private final class DetachViewStageController {
		@FXML private Stage detached;
		@FXML private Button apply;
		@FXML private CheckBox detachOperations;
		@FXML private CheckBox detachHistory;
		@FXML private CheckBox detachModelcheck;
		@FXML private CheckBox detachStats;
		@FXML private CheckBox detachAnimations;

		@FXML
		public void initialize() {
			currentStage.register(this.detached);
		}

		@FXML
		private void apply() {
			Parent root = loadPreset("draggable.fxml");
			Platform.runLater(() -> {
				BorderPane pane = (BorderPane) root.getChildrenUnmodifiable().get(0);
				Accordion accordion = (Accordion) pane.getChildren().get(0);
				for (TitledPane tp : accordion.getPanes()) {
					tp.managedProperty().bind(visibleProperty());
					if (tp.getContent() instanceof OperationsView) {
						tp.setVisible(!detachOperations.isSelected());
					} else if (tp.getContent() instanceof HistoryView) {
						tp.setVisible(!detachHistory.isSelected());
					} else if (tp.getContent() instanceof ModelcheckingController) {
						tp.setVisible(!detachModelcheck.isSelected());
					} else if (tp.getContent() instanceof StatsView) {
						tp.setVisible(!detachStats.isSelected());
					} else if (tp.getContent() instanceof AnimationsView){
						tp.setVisible(!detachAnimations.isSelected());
					}
				}
				this.detached.close();
			});
		}
	}
	private static final URL FXML_ROOT;
	
	static {
		try {
			FXML_ROOT = new URL(MenuController.class.getResource("menu.fxml"), "..");
		} catch (MalformedURLException e) {
			throw new IllegalStateException(e);
		}
	}
	
	private static final Logger logger = LoggerFactory.getLogger(MenuController.class);

	private final Injector injector;
	private final Api api;
	private final AnimationSelector animationSelector;
	private final CurrentStage currentStage;
	private final CurrentTrace currentTrace;
	private final RecentFiles recentFiles;
	private final DetachViewStageController dvController;
	
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
		final FXMLLoader loader,
		final Injector injector,
		final Api api,
		final AnimationSelector animationSelector,
		final CurrentStage currentStage,
		final CurrentTrace currentTrace,
		final RecentFiles recentFiles
	) {
		this.injector = injector;
		this.api = api;
		this.animationSelector = animationSelector;
		this.currentStage = currentStage;
		this.currentTrace = currentTrace;
		this.recentFiles = recentFiles;

		loader.setLocation(getClass().getResource("menu.fxml"));
		loader.setRoot(this);
		loader.setController(this);
		try {
			loader.load();
		} catch (IOException e) {
			logger.error("loading fxml failed", e);
		}

		if (System.getProperty("os.name", "").toLowerCase().contains("mac")) {
			// Mac-specific menu stuff
			this.setUseSystemMenuBar(true);
			final MenuToolkit tk = MenuToolkit.toolkit();
			
			// Remove About menu item from Help
			aboutItem.getParentMenu().getItems().remove(aboutItem);
			aboutItem.setText("About ProB 2");
			
			// Remove Preferences menu item from Edit
			preferencesItem.getParentMenu().getItems().remove(preferencesItem);
			preferencesItem.setAccelerator(KeyCombination.valueOf("Shortcut+,"));
			
			// Create Mac-style application menu
			final Menu applicationMenu = tk.createDefaultApplicationMenu("ProB 2");
			this.getMenus().add(0, applicationMenu);
			tk.setApplicationMenu(applicationMenu);
			applicationMenu.getItems().setAll(aboutItem, new SeparatorMenuItem(), preferencesItem,
				new SeparatorMenuItem(), tk.createHideMenuItem("ProB 2"), tk.createHideOthersMenuItem(),
				tk.createUnhideAllMenuItem(), new SeparatorMenuItem(), tk.createQuitMenuItem("ProB 2"));
			
			// Add Mac-style items to Window menu
			windowMenu.getItems().addAll(tk.createMinimizeMenuItem(), tk.createZoomMenuItem(),
				tk.createCycleWindowsItem(), new SeparatorMenuItem(), tk.createBringAllToFrontItem(),
				new SeparatorMenuItem());
			tk.autoAddWindowMenuItems(windowMenu);
			
			// Make this the global menu bar
			tk.setGlobalMenuBar(this);
		}

		final FXMLLoader stageLoader = injector.getInstance(FXMLLoader.class);
		stageLoader.setLocation(getClass().getResource("detachedPerspectivesChoice.fxml"));
		this.dvController = new DetachViewStageController();
		stageLoader.setController(this.dvController);
		try {
			stageLoader.load();
		} catch (IOException e) {
			logger.error("loading fxml failed", e);
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
			final List<MenuItem> newItems = new ArrayList<>();
			for (String s : this.recentFiles) {
				final MenuItem item = new MenuItem(new File(s).getName());
				item.setOnAction(event -> this.open(s));
				newItems.add(item);
			}
			
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
		loadPreset("main.fxml");
	}

	@FXML
	private void handleLoadSeparated() {
		loadPreset("separatedHistory.fxml");
	}

	@FXML
	private void handleLoadSeparated2() {
		loadPreset("separatedHistoryAndStatistics.fxml");
	}

	@FXML
	private void handleLoadStacked() {
		loadPreset("stackedLists.fxml");
	}

	@FXML
	private void handleLoadDetached() {
		this.dvController.detached.show();
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
				Parent root = loader.load();
				window.getScene().setRoot(root);
			} catch (IOException e) {
				logger.error("loading fxml failed", e);
				Alert alert = new Alert(Alert.AlertType.ERROR, "Could not open file:\n" + e);
				alert.getDialogPane().getStylesheets().add("prob.css");
				alert.showAndWait();
			}
		}
	}
	
	private void open(String path) {
		final StateSpace newSpace;
		try {
			newSpace = this.api.b_load(path);
		} catch (IOException | BException e) {
			logger.error("loading file failed", e);
			Alert alert = new Alert(Alert.AlertType.ERROR, "Could not open file:\n" + e);
			alert.getDialogPane().getStylesheets().add("prob.css");
			alert.showAndWait();
			return;
		}
		
		this.animationSelector.addNewAnimation(new Trace(newSpace));
		injector.getInstance(ModelcheckingController.class).resetView();
		
		// Remove the path first to avoid listing the same file twice.
		this.recentFiles.remove(path);
		this.recentFiles.add(0, path);
	}

	@FXML
	private void handleOpen(ActionEvent event) {
		final FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Open File");
		fileChooser.getExtensionFilters().addAll(
			// new FileChooser.ExtensionFilter("All Files", "*.*"),
			new FileChooser.ExtensionFilter("Classical B Files", "*.mch", "*.ref", "*.imp")// ,
			// new FileChooser.ExtensionFilter("EventB Files", "*.eventb", "*.bum", "*.buc"),
			// new FileChooser.ExtensionFilter("CSP Files", "*.cspm")
		);

		final File selectedFile = fileChooser.showOpenDialog(this.window);
		if (selectedFile == null) {
			return;
		}

		switch (fileChooser.getSelectedExtensionFilter().getDescription()) {
			case "Classical B Files":
				this.open(selectedFile.getAbsolutePath());
				break;
	
			default:
				throw new IllegalStateException(
						"Unknown file type selected: " + fileChooser.getSelectedExtensionFilter().getDescription());
		}
	}

	@FXML
	private void handleClose(final ActionEvent event) {
		final Stage stage = this.currentStage.get();
		if (stage != null) {
			stage.close();
		}
	}

	@FXML
	private void handlePreferences(ActionEvent event) {
		final Stage preferencesStage = injector.getInstance(PreferencesStage.class);
		preferencesStage.show();
		preferencesStage.toFront();
	}

	@FXML
	private void handleFormulaInput(ActionEvent event) {
		final Stage formulaInputStage = injector.getInstance(FormulaInputStage.class);
		formulaInputStage.show();
		formulaInputStage.toFront();
	}

	@FXML
	private void handleDotty(ActionEvent event) {
		injector.getInstance(DottyStage.class).showAndWait();
	}

	@FXML
	public void handleGroovyConsole(ActionEvent event) {
		final Stage groovyConsoleStage = injector.getInstance(GroovyConsoleStage.class);
		groovyConsoleStage.show();
		groovyConsoleStage.toFront();
	}
	
	@FXML
	public void handleBConsole(ActionEvent event) {
		final Stage bConsoleStage = injector.getInstance(BConsoleStage.class);
		bConsoleStage.show();
		bConsoleStage.toFront();
	}

	private Parent loadPreset(String location) {
		FXMLLoader loader = injector.getInstance(FXMLLoader.class);
		try {
			loader.setLocation(new URL(FXML_ROOT, location));
		} catch (MalformedURLException e) {
			logger.error("Malformed location", e);
			Alert alert = new Alert(Alert.AlertType.ERROR, "Malformed location:\n" + e);
			alert.getDialogPane().getStylesheets().add("prob.css");
			alert.showAndWait();
			return null;
		}
		Parent root;
		try {
			root = loader.load();
		} catch (IOException e) {
			logger.error("loading fxml failed", e);
			Alert alert = new Alert(Alert.AlertType.ERROR, "Could not open file:\n" + e);
			alert.getDialogPane().getStylesheets().add("prob.css");
			alert.showAndWait();
			return null;
		}
		window.getScene().setRoot(root);

		if (System.getProperty("os.name", "").toLowerCase().contains("mac")) {
			final MenuToolkit tk = MenuToolkit.toolkit();
			tk.setGlobalMenuBar(this);
			tk.setApplicationMenu(this.getMenus().get(0));
		}
		return root;
	}
	
	@FXML
	private void handleReportBug(ActionEvent event) {
		WebView webView = new WebView();
		WebEngine webEnging = webView.getEngine();
		webEnging.setJavaScriptEnabled(true);
		webEnging.load("https://probjira.atlassian.net/secure/RapidBoard.jspa?rapidView=8");
		
		Scene scene = new Scene(webView);
		scene.getStylesheets().add("prob.css");
		
		Stage stage = new Stage();
		stage.setScene(scene);
		stage.setTitle("Report Bug");
		stage.show();
	}

}
