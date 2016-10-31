package de.prob2.ui.consoles.groovy;

import java.io.IOException;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.prob2fx.CurrentStage;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class GroovyConsoleStage extends Stage {
	private static final Logger logger = LoggerFactory.getLogger(GroovyConsoleStage.class);

	private GroovyConsole groovyConsole;
		
	@Inject
	private GroovyConsoleStage(FXMLLoader loader, CurrentStage currentStage, GroovyConsole groovyConsole) {
		try {
			loader.setLocation(getClass().getResource("groovy_console_stage.fxml"));
			loader.setRoot(this);
			loader.setController(this);
			loader.load();
		} catch (IOException e) {
			logger.error("loading fxml failed", e);
		}
		currentStage.register(this);
		this.getScene().setRoot(new StackPane(groovyConsole));
	}
	
	@FXML
	public void initialize() {
		this.setOnCloseRequest(e -> groovyConsole.closeObjectStage());
	}
}
