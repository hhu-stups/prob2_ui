package de.prob2.ui.menu;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Optional;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import de.be4.classicalb.core.parser.exceptions.BException;
import de.codecentric.centerdevice.MenuToolkit;
import de.prob.scripting.Api;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import de.prob2.ui.dotty.DottyStage;
import de.prob2.ui.formula.FormulaGenerator;
import de.prob2.ui.groovy.GroovyConsoleStage;
import de.prob2.ui.modelchecking.ModelcheckingController;
import de.prob2.ui.modelchecking.ModelcheckingStage;
import de.prob2.ui.preferences.PreferencesStage;
import de.prob2.ui.prob2fx.CurrentStage;
import de.prob2.ui.prob2fx.CurrentTrace;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.KeyCombination;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;

@Singleton
public final class MenuController extends MenuBar {
	private final Injector injector;
	private final Api api;
	private final AnimationSelector animationSelector;
	private final CurrentStage currentStage;
	private final CurrentTrace currentTrace;
	private final FormulaGenerator formulaGenerator;
	private final ModelcheckingController modelcheckingController;
	
	private final DottyStage dottyStage;
	private final Stage mcheckStage;
	private final PreferencesStage preferencesStage;
	private final GroovyConsoleStage groovyConsoleStage;
	
	private Window window;
	
	@FXML private Menu windowMenu;
	@FXML private MenuItem preferencesItem;
	@FXML private MenuItem enterFormulaForVisualization;
	@FXML private MenuItem aboutItem;

	@FXML
	private void handleLoadDefault() {
		FXMLLoader loader = injector.getInstance(FXMLLoader.class);
		loader.setLocation(getClass().getResource("../main.fxml"));
		try {
			loader.load();
		} catch (IOException e) {
			System.err.println("Failed to load FXML-File!");
			e.printStackTrace();
		}
		Parent root = loader.getRoot();
		Scene scene = new Scene(root, window.getWidth(), window.getHeight());
		((Stage) window).setScene(scene);
	}

	@FXML
	private void handleLoadPerspective() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Open File");
		fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("FXML Files", "*.fxml"));
		File selectedFile = fileChooser.showOpenDialog(window);
		if (selectedFile != null)
			try {
				FXMLLoader loader = injector.getInstance(FXMLLoader.class);
				loader.setLocation(new URL("file://" + selectedFile.getPath()));
				loader.load();
				Parent root = loader.getRoot();
				Scene scene = new Scene(root, window.getHeight(), window.getWidth());
				((Stage) window).setScene(scene);
			} catch (IOException e) {
				System.err.println("Failed to load FXML-File!");
				e.printStackTrace();
			}
	}

	@FXML
	private void handleOpen(ActionEvent event) {
		final FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Open File");
		fileChooser.getExtensionFilters().addAll(
				// new FileChooser.ExtensionFilter("All Files", "*.*"),
				new FileChooser.ExtensionFilter("Classical B Files", "*.mch", "*.ref", "*.imp")// ,
		// new FileChooser.ExtensionFilter("EventB Files", "*.eventb", "*.bum",
		// "*.buc"),
		// new FileChooser.ExtensionFilter("CSP Files", "*.cspm")
		);

		final File selectedFile = fileChooser.showOpenDialog(this.window);
		if (selectedFile == null) {
			return;
		}

		switch (fileChooser.getSelectedExtensionFilter().getDescription()) {
		case "Classical B Files":
			final StateSpace newSpace;
			try {
				newSpace = this.api.b_load(selectedFile.getAbsolutePath());
			} catch (IOException | BException e) {
				e.printStackTrace();
				Alert alert = new Alert(Alert.AlertType.ERROR, "Could not open file:\n" + e);
				alert.getDialogPane().getStylesheets().add("prob.css");
				alert.showAndWait();
				return;
			}

			this.animationSelector.addNewAnimation(new Trace(newSpace));
			modelcheckingController.resetView();
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
		this.preferencesStage.show();
		this.preferencesStage.toFront();
	}

	@FXML
	private void handleFormulaInput(ActionEvent event) {
		TextInputDialog dialog = new TextInputDialog();
		dialog.setTitle("Enter Formula for Visualization");
		dialog.setHeaderText("Enter Formula for Visualization");
		dialog.setContentText("Enter Formula: ");
		dialog.getDialogPane().getStylesheets().add("prob.css");
		Optional<String> result = dialog.showAndWait();
		if (result.isPresent()) {
			formulaGenerator.parseAndShowFormula(result.get());
		}
	}

	@FXML
	private void handleModelCheck(ActionEvent event) {
		this.mcheckStage.showAndWait();
		this.mcheckStage.toFront();
	}
	
	@FXML
	private void handleDotty(ActionEvent event) {
		this.dottyStage.showAndWait();
	}

	@FXML
	public void handleGroovyConsole(ActionEvent event) {
		this.groovyConsoleStage.show();
		this.groovyConsoleStage.toFront();
	}
	
	
	@FXML
	public void initialize() {
		this.sceneProperty().addListener((observable, from, to) -> {
			if (to != null) {
				to.windowProperty().addListener((observable1, from1, to1) -> {
					this.window = to1;
					this.mcheckStage.initOwner(this.window);
				});
			}
		});
		
		this.enterFormulaForVisualization.disableProperty().bind(currentTrace.currentStateProperty().initializedProperty().not());
	}

	@Inject

	private MenuController(
		final FXMLLoader loader,
		
		final Injector injector,
		final Api api,
		final AnimationSelector animationSelector,
		final CurrentStage currentStage,
		final CurrentTrace currentTrace,
		final FormulaGenerator formulaGenerator,
		final ModelcheckingController modelcheckingController,
		
		final DottyStage dottyStage,
		final GroovyConsoleStage groovyConsoleStage,
		final ModelcheckingStage modelcheckingStage,
		final PreferencesStage preferencesStage
	) {
		this.injector = injector;
		this.api = api;
		this.animationSelector = animationSelector;
		this.currentStage = currentStage;
		this.currentTrace = currentTrace;
		this.formulaGenerator = formulaGenerator;
		this.modelcheckingController = modelcheckingController;
		
		this.dottyStage = dottyStage;
		this.groovyConsoleStage = groovyConsoleStage;
		this.mcheckStage = modelcheckingStage;
		this.preferencesStage = preferencesStage;
		
		loader.setLocation(getClass().getResource("menu.fxml"));
		loader.setRoot(this);
		loader.setController(this);
		try {
			loader.load();
		} catch (IOException e) {
			e.printStackTrace();
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
			applicationMenu.getItems().setAll(
				aboutItem,
				new SeparatorMenuItem(),
				preferencesItem,
				new SeparatorMenuItem(),
				tk.createHideMenuItem("ProB 2"),
				tk.createHideOthersMenuItem(),
				tk.createUnhideAllMenuItem(),
				new SeparatorMenuItem(),
				tk.createQuitMenuItem("ProB 2")
			);

			// Add Mac-style items to Window menu
			windowMenu.getItems().addAll(
				tk.createMinimizeMenuItem(),
				tk.createZoomMenuItem(),
				tk.createCycleWindowsItem(),
				new SeparatorMenuItem(),
				tk.createBringAllToFrontItem(),
				new SeparatorMenuItem()
			);
			tk.autoAddWindowMenuItems(windowMenu);

			// Make this the global menu bar
			tk.setGlobalMenuBar(this);
		}
	}

}
