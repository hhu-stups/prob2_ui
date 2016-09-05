package de.prob2.ui.groovy;

import java.io.IOException;

import com.google.inject.Inject;

import com.google.inject.Singleton;
import de.prob2.ui.prob2fx.CurrentStage;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;

@Singleton
public final class GroovyConsoleStage extends Stage {

	@FXML
	private GroovyConsole groovyConsole;

	@Inject
	private GroovyConsoleStage(FXMLLoader loader, CurrentStage currentStage, GroovyInterpreter interpreter) {
		try {
			loader.setLocation(getClass().getResource("groovy_console_stage.fxml"));
			loader.setRoot(this);
			loader.setController(this);
			loader.load();
		} catch (IOException e) {
			e.printStackTrace();
		}
		groovyConsole.setInterpreter(interpreter);
		this.setOnCloseRequest(e-> {
			groovyConsole.closeObjectStage();
		});
		
		currentStage.register(this);
	}
}
