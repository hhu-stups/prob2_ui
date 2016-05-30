package de.prob2.ui.modeline;

import java.io.IOException;

import com.google.inject.Inject;

import de.prob.scripting.Api;
import de.prob.statespace.Animations;
import de.prob2.ui.Database;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.Pane;

public class ModelineController extends Pane {

	@Inject
	public ModelineController(FXMLLoader loader, Api api, Animations animations, Database db) {
		System.out.println("Hi");
		try {
			loader.setLocation(getClass().getResource("modeline.fxml"));
			loader.setRoot(this);
			loader.load();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
