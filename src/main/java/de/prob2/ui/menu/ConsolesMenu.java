package de.prob2.ui.menu;

import com.google.inject.Inject;
import com.google.inject.Injector;

import de.prob2.ui.consoles.b.BConsoleStage;
import de.prob2.ui.consoles.groovy.GroovyConsoleStage;
import de.prob2.ui.internal.StageManager;
import javafx.fxml.FXML;
import javafx.scene.control.Menu;
import javafx.stage.Stage;

public class ConsolesMenu extends Menu {

	private final Injector injector;

	@Inject
	private ConsolesMenu(final StageManager stageManager, final Injector injector) {
		this.injector = injector;
		stageManager.loadFXML(this, "consolesMenu.fxml");
	}

	@FXML
	private void handleGroovyConsole() {
		final Stage groovyConsoleStage = injector.getInstance(GroovyConsoleStage.class);
		groovyConsoleStage.show();
		groovyConsoleStage.toFront();
	}

	@FXML
	private void handleBConsole() {
		final Stage bConsoleStage = injector.getInstance(BConsoleStage.class);
		bConsoleStage.show();
		bConsoleStage.toFront();
	}
}
