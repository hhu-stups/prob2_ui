package de.prob2.ui.menu;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Optional;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.be4.classicalb.core.parser.exceptions.BException;
import de.codecentric.centerdevice.MenuToolkit;
import de.prob.scripting.Api;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import de.prob2.ui.ProB2;
import de.prob2.ui.dotty.DottyStage;
import de.prob2.ui.formula.FormulaGenerator;
import de.prob2.ui.modelchecking.ModelcheckingController;
import de.prob2.ui.modelchecking.ModelcheckingStage;
import de.prob2.ui.preferences.PreferencesStage;
import de.prob2.ui.states.BlacklistStage;
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
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;

@Singleton
public class MenuController extends MenuBar {
	private final Api api;
	private final AnimationSelector animationSelector;
	private final BlacklistStage blacklistStage;
	private final PreferencesStage preferencesStage;
	private final ModelcheckingController modelcheckingController;
	private final Stage mcheckStage;
	private final FormulaGenerator formulaGenerator;
	private Window window;
	private DottyStage dottyStage;

	@FXML
	private void handleLoadDefault() {
		Window stage = this.getScene().getWindow();
		try {
			FXMLLoader loader = ProB2.injector.getInstance(FXMLLoader.class);
			loader.setLocation(getClass().getResource("../main.fxml"));
			loader.load();
			Parent root = loader.getRoot();
			Scene scene = new Scene(root, stage.getHeight(), stage.getWidth());
			((Stage) window).setScene(scene);
		} catch (IOException e) {
			System.err.println("Failed to load FXML-File!");
			e.printStackTrace();
		}
	}

	@FXML
	private void handleLoadPerspective() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Open File");
		fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("FXML Files", "*.fxml"));
		File selectedFile = fileChooser.showOpenDialog(window);
		if (selectedFile != null)
			try {
				FXMLLoader loader = ProB2.injector.getInstance(FXMLLoader.class);
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
				new Alert(Alert.AlertType.ERROR, "Could not open file:\n" + e).showAndWait();
				return;
			}

			final Trace currentTrace = this.animationSelector.getCurrentTrace();
			if (currentTrace != null) {
				this.animationSelector.removeTrace(currentTrace);
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
	private void handleEditBlacklist(ActionEvent event) {
		this.blacklistStage.show();
		this.blacklistStage.toFront();
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
		dialog.setHeaderText("Enter Formula for Vistualization");
		dialog.setContentText("Enter Formula: ");
		Optional<String> result = dialog.showAndWait();
		if (result.isPresent()) {
			formulaGenerator.setFormula(formulaGenerator.parse(result.get()));
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
	public void initialize() {
		this.sceneProperty().addListener((observable, from, to) -> {
			if (to != null) {
				to.windowProperty().addListener((observable1, from1, to1) -> {
					this.window = to1;
					this.mcheckStage.initOwner(this.window);
				});
			}
		});
	}

	@Inject
	private MenuController(final FXMLLoader loader, final Api api, final AnimationSelector animationSelector,
			final BlacklistStage blacklistStage, final PreferencesStage preferencesStage,
			final ModelcheckingStage modelcheckingStage, final ModelcheckingController modelcheckingController,
			final FormulaGenerator formulaGenerator, final DottyStage dottyStage) {
		this.api = api;
		this.animationSelector = animationSelector;
		this.blacklistStage = blacklistStage;
		this.preferencesStage = preferencesStage;
		this.formulaGenerator = formulaGenerator;
		this.modelcheckingController = modelcheckingController;
		this.mcheckStage = modelcheckingStage;
		this.dottyStage = dottyStage;
		try {
			loader.setLocation(getClass().getResource("menu.fxml"));
			loader.setRoot(this);
			loader.setController(this);
			loader.load();
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (System.getProperty("os.name", "").toLowerCase().contains("mac")) {
			// Mac-specific menu stuff
			this.setUseSystemMenuBar(true);
			MenuToolkit tk = MenuToolkit.toolkit();

			// Create Mac-style application menu
			Menu applicationMenu = tk.createDefaultApplicationMenu("ProB 2");
			this.getMenus().add(0, applicationMenu);
			tk.setApplicationMenu(applicationMenu);

			// Move About menu item from Help to application menu
			Menu helpMenu = this.getMenus().get(this.getMenus().size() - 1);
			MenuItem aboutItem = helpMenu.getItems().get(helpMenu.getItems().size() - 1);
			aboutItem.setText("About ProB 2");
			helpMenu.getItems().remove(aboutItem);
			applicationMenu.getItems().set(0, aboutItem);

			// Create Mac-style Window menu
			Menu windowMenu = new Menu("Window");
			windowMenu.getItems().addAll(tk.createMinimizeMenuItem(), tk.createZoomMenuItem(),
					tk.createCycleWindowsItem(), new SeparatorMenuItem(), tk.createBringAllToFrontItem(),
					new SeparatorMenuItem());
			tk.autoAddWindowMenuItems(windowMenu);
			this.getMenus().add(this.getMenus().size() - 1, windowMenu);

			// Make this the global menu bar
			tk.setGlobalMenuBar(this);
		}
	}

}
