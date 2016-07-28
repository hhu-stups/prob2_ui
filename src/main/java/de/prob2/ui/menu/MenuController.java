package de.prob2.ui.menu;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.codecentric.centerdevice.MenuToolkit;
import de.prob2.ui.ProB2;
import de.prob2.ui.events.OpenFileEvent;
import de.prob2.ui.modelchecking.ModelcheckingDialog;
import de.prob2.ui.preferences.PreferencesStage;
import de.prob2.ui.states.BlacklistStage;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import javafx.stage.Window;

@Singleton
public class MenuController extends MenuBar {
	private EventBus bus;
	private BlacklistStage blacklistStage;
	private PreferencesStage preferencesStage;
	private Scene mcheckScene;
	private Window window;

	@FXML
	private void handleLoadDefault(){
		Window stage = this.getScene().getWindow();
		try {
			FXMLLoader loader = ProB2.injector.getInstance(FXMLLoader.class);
			loader.setLocation(getClass().getResource("../main.fxml"));
			loader.load();
			Parent root = loader.getRoot();
			Scene scene = new Scene(root,stage.getHeight(),stage.getWidth());
			((Stage) stage).setScene(scene);
		} catch (IOException e) {
			System.err.println("Failed to load FXML-File!");
			e.printStackTrace();
		}
	}
	
	@FXML
	private void handleLoadPerspective(){
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Open File");
		fileChooser.getExtensionFilters().addAll(new ExtensionFilter("FXML Files", "*.fxml"));
		File selectedFile = fileChooser.showOpenDialog(window);
		if (selectedFile!=null)
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
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Open File");
		fileChooser.getExtensionFilters().addAll(
				// new ExtensionFilter("All Files", "*.*"),
				new ExtensionFilter("Classical B Files", "*.mch", "*.ref", "*.imp")
				// new ExtensionFilter("EventB Files", "*.eventb", "*.bum", "*.buc"),
				// new ExtensionFilter("CSP Files", "*.cspm")
		);
		bus.post(fileChooser);
	}
	
	@FXML
	private void handleEditBlacklist(ActionEvent event) {
		this.blacklistStage.show();
	}
	
	@FXML
	private void handlePreferences(ActionEvent event) {
		this.preferencesStage.show();
	}
	
	@FXML
	private void handleModelCheck(ActionEvent event) {
		Stage mcheckStage = new Stage();
		mcheckStage.setTitle("Model Check");
		mcheckStage.initOwner(this.window);
		mcheckStage.setScene(mcheckScene);
		mcheckStage.showAndWait();
	}

	@Subscribe
	public void showFileDialogHandler(FileChooser chooser) {
		File selectedFile = chooser.showOpenDialog(this.window);
		if (selectedFile != null) {
			String extensionFilter = chooser.getSelectedExtensionFilter().getDescription();
			bus.post(new OpenFileEvent(selectedFile, extensionFilter));
		}
	}
	
	@FXML
	public void initialize() {
		this.sceneProperty().addListener((observable, from, to) -> {
			if (to != null) {
				to.windowProperty().addListener((observable1, from1, to1) -> {
					this.window = to1;
				});
			}
		});
	}

	@Inject
	private MenuController(
		FXMLLoader loader,
		EventBus bus,
		BlacklistStage blacklistStage,
		PreferencesStage preferencesStage,
		ModelcheckingDialog mcheckController
	) {
		this.bus = bus;
		this.blacklistStage = blacklistStage;
		this.preferencesStage = preferencesStage;
		this.mcheckScene = new Scene(mcheckController);
		try {
			loader.setLocation(getClass().getResource("menu.fxml"));
			loader.setRoot(this);
			loader.setController(this);
			loader.load();
		} catch (IOException e) {
			e.printStackTrace();
		}
		bus.register(this);
		
		if (System.getProperty("os.name", "").toLowerCase().contains("mac")) {
			// Mac-specific menu stuff
			this.setUseSystemMenuBar(true);
			MenuToolkit tk = MenuToolkit.toolkit();
			
			// Create Mac-style application menu
			Menu applicationMenu = tk.createDefaultApplicationMenu("ProB 2");
			this.getMenus().add(0, applicationMenu);
			tk.setApplicationMenu(applicationMenu);
			
			// Move About menu item from Help to application menu
			Menu helpMenu = this.getMenus().get(this.getMenus().size()-1);
			MenuItem aboutItem = helpMenu.getItems().get(helpMenu.getItems().size()-1);
			aboutItem.setText("About ProB 2");
			helpMenu.getItems().remove(aboutItem);
			applicationMenu.getItems().set(0, aboutItem);
			
			// Create Mac-style Window menu
			Menu windowMenu = new Menu("Window");
			windowMenu.getItems().addAll(
				tk.createMinimizeMenuItem(),
				tk.createZoomMenuItem(),
				tk.createCycleWindowsItem(),
				new SeparatorMenuItem(),
				tk.createBringAllToFrontItem(),
				new SeparatorMenuItem()
			);
			tk.autoAddWindowMenuItems(windowMenu);
			this.getMenus().add(this.getMenus().size()-1, windowMenu);
			
			// Make this the global menu bar
			tk.setGlobalMenuBar(this);
		}
	}

}
