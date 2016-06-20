package de.prob2.ui.menu;

import java.io.File;
import java.io.IOException;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.scripting.Api;
import de.prob.statespace.Animations;
import de.prob2.ui.events.OpenFileEvent;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.MenuBar;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Window;

@Singleton
public class MenuController extends MenuBar {

	private EventBus bus;

	@FXML
	private void handleOpen(ActionEvent event) {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Open File");
		fileChooser.getExtensionFilters().addAll(
				//new ExtensionFilter("All Files", "*.*"),
				new ExtensionFilter("Classical B Files", "*.mch", "*.ref", "*.imp")
		// new ExtensionFilter("EventB Files", "*.eventb", "*.bum", "*.buc"),
		// new ExtensionFilter("CSP Files", "*.cspm")
		);
		bus.post(fileChooser);
	}
	
	@FXML
	private void handleModelCheck(ActionEvent event) {
		
	}

	@Subscribe
	public void showFileDialogHandler(FileChooser chooser) {
		Window stage = this.getScene().getWindow();
		File selectedFile = chooser.showOpenDialog(stage);
		if (selectedFile != null) {
			String extensionFilter = chooser.getSelectedExtensionFilter().getDescription();
			bus.post(new OpenFileEvent(selectedFile, extensionFilter));
		}
	}

	@Inject
	public MenuController(FXMLLoader loader, Api api, EventBus bus, Animations animations) {
		this.bus = bus;
		try {
			loader.setLocation(getClass().getResource("menu.fxml"));
			loader.setRoot(this);
			loader.setController(this);
			loader.load();
		} catch (IOException e) {
			e.printStackTrace();
		}
		bus.register(this);
	}

}
