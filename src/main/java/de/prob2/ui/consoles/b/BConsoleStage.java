package de.prob2.ui.consoles.b;

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
public final class BConsoleStage extends Stage {
	private static final Logger logger = LoggerFactory.getLogger(BConsoleStage.class);

	@FXML
	private BConsole bConsole;

	@Inject
	private BConsoleStage(FXMLLoader loader, CurrentStage currentStage) {
		try {
			loader.setLocation(getClass().getResource("b_console_stage.fxml"));
			loader.setRoot(this);
			loader.setController(this);
			loader.load();
		} catch (IOException e) {
			logger.error("loading fxml failed", e);
		}
		currentStage.register(this);
	}
}
