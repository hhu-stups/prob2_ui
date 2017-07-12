package de.prob2.ui.menu;

import com.google.inject.Inject;
import com.google.inject.Injector;

import de.prob2.ui.helpsystem.HelpSystemStage;
import de.prob2.ui.internal.StageManager;
import javafx.fxml.FXML;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.stage.Stage;

public class HelpMenu extends Menu {

	@FXML
	private MenuItem aboutItem;
	
	private final Injector injector;

	@Inject
	private HelpMenu(final StageManager stageManager, final Injector injector) {
		this.injector = injector;
		stageManager.loadFXML(this, "helpMenu.fxml");
	}

	@FXML
	private void handleOpenHelp() {
		final Stage helpSystemStage = injector.getInstance(HelpSystemStage.class);
		helpSystemStage.show();
		helpSystemStage.toFront();
	}
	
	@FXML
	private void handleAboutDialog() {
		injector.getInstance(AboutBoxController.class).showAndWait();
	}

	@FXML
	private void handleReportBug() {
		final Stage reportBugStage = injector.getInstance(ReportBugStage.class);
		reportBugStage.show();
		reportBugStage.toFront();
	}

	MenuItem getAboutItem() {
		return this.aboutItem;
	}
}
