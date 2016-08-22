package de.prob2.ui.groovy;

import java.io.IOException;

import com.google.inject.Inject;

import de.prob.statespace.AnimationSelector;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.AnchorPane;


public class GroovyConsoleView extends AnchorPane {
	
	private AnimationSelector animations;
	
	//private ScriptEngineProvider provider;
	
	private GroovyInterpreter interpreter;

	@FXML
	private GroovyConsole groovyConsole;
	
	@Inject
	private GroovyConsoleView(FXMLLoader loader, AnimationSelector animations, GroovyInterpreter interpreter) {
		this.animations = animations;
		//this.provider = provider;
		try {
			loader.setLocation(getClass().getResource("groovy_console_view.fxml"));
			loader.setRoot(this);
			loader.setController(this);
			loader.load();
		} catch (IOException e) {
			e.printStackTrace();
		}
		groovyConsole.setInterpreter(interpreter);
		//this.interpreter = interpreter;
	}
}
