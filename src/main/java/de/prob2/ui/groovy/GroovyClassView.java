package de.prob2.ui.groovy;

import java.io.IOException;

import com.google.inject.Inject;

import javafx.fxml.FXMLLoader;
import javafx.scene.layout.AnchorPane;

public class GroovyClassView extends AnchorPane {
	
	@Inject
	private GroovyClassView(FXMLLoader loader) {
		loader.setLocation(getClass().getResource("groovy_class_view.fxml"));
		loader.setRoot(this);
		loader.setController(this);
		try {
			loader.load();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
