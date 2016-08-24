package de.prob2.ui.groovy;

import java.io.IOException;

import com.google.inject.Inject;

import javafx.fxml.FXMLLoader;
import javafx.scene.layout.AnchorPane;

public class GroovyObjectView extends AnchorPane {
	
	
	@Inject
	private GroovyObjectView(FXMLLoader loader) {
		try {
			loader.setLocation(getClass().getResource("groovy_object_view.fxml"));
			loader.setRoot(this);
			loader.setController(this);
			loader.load();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
