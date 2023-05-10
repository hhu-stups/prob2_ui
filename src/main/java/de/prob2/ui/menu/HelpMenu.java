package de.prob2.ui.menu;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import com.google.common.io.CharStreams;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
		handleSyntax("prob_summary.txt", "B");
	}

	@FXML
	private void handleZSyntax() {
		handleSyntax("proz_summary.txt", "Z");
	}

	@FXML
	private void handleCSPSyntax() {
		handleSyntax("procsp_summary.txt", "CSP");
	}
	@FXML
	private void handleTLASyntax() {
		handleSyntax("tla_summary.txt", "TLA");
	}

	@FXML
	private void handleLTLSyntax() {
		handleSyntax("ltl_summary.txt", "LTL");
	}

	private void handleSyntax(String filename, String title) {
		SyntaxStage syntaxStage = injector.getInstance(SyntaxStage.class);
		syntaxStage.setTitle(title);
		try (
			InputStream is = Objects.requireNonNull(this.getClass().getResourceAsStream(filename));
			InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
		) {
			syntaxStage.setText(CharStreams.toString(isr));
		} catch (IOException | UncheckedIOException e) {
			LOGGER.error("Could not read syntax help file: {}", filename, e);
			stageManager.makeExceptionAlert(e, "common.alerts.couldNotOpenFile.content", filename).show();
			return;
		}
		syntaxStage.show();
	}

	MenuItem getAboutItem() {
		return this.aboutItem;
	}
}
