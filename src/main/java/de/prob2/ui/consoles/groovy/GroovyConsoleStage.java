package de.prob2.ui.consoles.groovy;

import java.io.IOException;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.prob2fx.CurrentStage;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class GroovyConsoleStage extends Stage {
	private static final Logger logger = LoggerFactory.getLogger(GroovyConsoleStage.class);

	@FXML private GroovyConsole groovyConsole;
	
	private final GroovyInterpreter interpreter;

	@Inject
	private GroovyConsoleStage(FXMLLoader loader, CurrentStage currentStage, GroovyInterpreter interpreter) {
		this.interpreter = interpreter;
		
		try {
			loader.setLocation(getClass().getResource("groovy_console_stage.fxml"));
			loader.setRoot(this);
			loader.setController(this);
			loader.load();
		} catch (IOException e) {
			logger.error("loading fxml failed", e);
		}
		
		currentStage.register(this);
	}
	
	@FXML
	public void initialize() {
		groovyConsole.setInterpreter(interpreter);
		this.setOnCloseRequest(e -> groovyConsole.closeObjectStage());
	}
}
