package de.prob2.ui.modeline;

import java.io.IOException;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import javafx.fxml.FXMLLoader;
import javafx.scene.layout.Pane;

@Singleton
public class ModelineController extends Pane {

	@Inject
	private ModelineController(FXMLLoader loader) {
		try {
			loader.setLocation(getClass().getResource("modeline.fxml"));
			loader.setRoot(this);
			loader.load();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
