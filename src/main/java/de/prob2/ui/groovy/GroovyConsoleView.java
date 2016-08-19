package de.prob2.ui.groovy;

import java.io.IOException;
import com.google.inject.Inject;
import de.prob.statespace.AnimationSelector;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.AnchorPane;


public class GroovyConsoleView extends AnchorPane {
	
	private AnimationSelector animations;

	@FXML
	private GroovyConsole groovyConsole;
	
	@Inject
	private GroovyConsoleView(FXMLLoader loader, AnimationSelector animations) {
		this.animations = animations;
		try {
			loader.setLocation(getClass().getResource("groovy_console_view.fxml"));
			loader.setRoot(this);
			loader.setController(this);
			loader.load();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
