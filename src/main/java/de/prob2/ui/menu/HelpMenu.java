package de.prob2.ui.menu;

import com.google.inject.Inject;
import com.google.inject.Injector;

import de.prob2.ui.ProB2;
import de.prob2.ui.helpsystem.HelpSystemStage;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import javafx.fxml.FXML;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.stage.Stage;

@FXMLInjected
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
		final Stage aboutBox = injector.getInstance(AboutBox.class);
		aboutBox.show();
		aboutBox.toFront();
	}

	@FXML
	private void handleReportBug() {
		injector.getInstance(ProB2.class).getHostServices().showDocument("https://github.com/hhu-stups/prob-issues/issues/new/choose");
	}

	MenuItem getAboutItem() {
		return this.aboutItem;
	}
}
