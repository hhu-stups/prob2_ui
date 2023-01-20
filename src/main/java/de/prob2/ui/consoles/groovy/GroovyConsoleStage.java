package de.prob2.ui.consoles.groovy;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.internal.StageManager;

import javafx.fxml.FXML;
import javafx.stage.Stage;


@Singleton
public final class GroovyConsoleStage extends Stage {

	@FXML
	private GroovyConsole groovyConsole;

	@Inject
	private GroovyConsoleStage(StageManager stageManager) {
		stageManager.loadFXML(this, "groovy_console_view.fxml", this.getClass().getName());
	}

	@FXML
	private void initialize() {
		this.setOnCloseRequest(e -> groovyConsole.closeObjectStage());
	}
}
