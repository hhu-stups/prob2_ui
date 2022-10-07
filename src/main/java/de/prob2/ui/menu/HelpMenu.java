package de.prob2.ui.menu;

import com.google.inject.Inject;
import com.google.inject.Injector;

import de.prob2.ui.ProB2;
import de.prob2.ui.helpsystem.HelpSystemStage;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Objects;

@FXMLInjected
public class HelpMenu extends Menu {
	@FXML
	private MenuItem aboutItem;
	
	private final Injector injector;
	private final StageManager stageManager;
	private static final Logger LOGGER = LoggerFactory.getLogger(HelpMenu.class);

	@Inject
	private HelpMenu(final StageManager stageManager, final Injector injector) {
		this.stageManager = stageManager;
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
		injector.getInstance(ProB2.class).getHostServices().showDocument(ProB2.BUG_REPORT_URL);
	}

	@FXML
	private void handleBSyntax() {
		handleSyntax("prob_summary.txt");
	}

	@FXML
	private void handleZSyntax() {
		handleSyntax("proz_summary.txt");
	}

	@FXML
	private void handleCSPSyntax() {
		handleSyntax("procsp_summary.txt");
	}
	@FXML
	private void handleTLASyntax() {
		handleSyntax("tla_summary.txt");
	}

	@FXML
	private void handleLTLSyntax() {
		handleSyntax("ltl_summary.txt");
	}

	private void handleSyntax(String filename) {
		SyntaxStage syntaxStage = injector.getInstance(SyntaxStage.class);
		try {
			syntaxStage.setContent(Paths.get(Objects.requireNonNull(this.getClass().getResource(filename)).toURI()));
		} catch (URISyntaxException e) {
			LOGGER.error("Could not create URI of {}", this.getClass().getResource(filename), e);
			final Alert alert = stageManager.makeExceptionAlert(e, "common.alerts.couldNotCreateURI", this.getClass().getResource(filename));
			alert.show();
			return;
		}
		syntaxStage.show();
	}

	MenuItem getAboutItem() {
		return this.aboutItem;
	}
}
