package de.prob2.ui.consoles.b;

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
public final class BConsoleStage extends Stage {
	private static final Logger logger = LoggerFactory.getLogger(BConsoleStage.class);

	@FXML
	private BConsole bConsole;

	@Inject
	@SuppressWarnings(value = "UR_UNINIT_READ", justification = "Field values are injected by FXMLLoader")
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
