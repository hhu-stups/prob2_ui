package de.prob2.ui.groovy;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.prob2fx.CurrentStage;
import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;

@Singleton
public final class GroovyConsoleStage extends Stage {

	@FXML
	private GroovyConsole groovyConsole;
	private Logger logger = LoggerFactory.getLogger(GroovyConsoleStage.class);

	@Inject
	@SuppressWarnings(value = "UR_UNINIT_READ", justification = "Field values are injected by FXMLLoader")
	private GroovyConsoleStage(FXMLLoader loader, CurrentStage currentStage, GroovyInterpreter interpreter, GroovyCodeCompletion codeCompletion) {
		try {
			loader.setLocation(getClass().getResource("groovy_console_stage.fxml"));
			loader.setRoot(this);
			loader.setController(this);
			loader.load();
		} catch (IOException e) {
			logger.error("loading fxml failed", e);
		}
		groovyConsole.setInterpreter(interpreter);
		groovyConsole.setCodeCompletion(codeCompletion);
		this.setOnCloseRequest(e -> {
			groovyConsole.closeObjectStage();
		});

		currentStage.register(this);
	}
}
