package de.prob2.ui.helpsystem;

import java.io.File;
import java.util.ResourceBundle;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.internal.StageManager;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class HelpSystemStage extends Stage {
	private static final Logger LOGGER = LoggerFactory.getLogger(HelpSystemStage.class);

	@Inject
	private HelpSystemStage(final StageManager stageManager, ResourceBundle bundle, final HelpSystem help) {
		this.setTitle(bundle.getString("helpsystem.stage.title"));
		this.setScene(new Scene(help));
		stageManager.register(this, this.getClass().getName());
		final File defaultPage = new File(help.getHelpSubdirectory(), "ProB2UI.html");
		setContent(defaultPage,"");
	}

	public void setContent(File file, String anchor) {
		final String url = file.toURI() + anchor;
		LOGGER.debug("Opening URL in help: {}", url);
		Platform.runLater(() -> ((HelpSystem) this.getScene().getRoot()).webEngine.load(url));
	}
}
