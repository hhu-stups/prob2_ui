package de.prob2.ui.helpsystem;

import java.io.File;
import java.util.ResourceBundle;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.internal.StageManager;

import javafx.scene.Scene;
import javafx.stage.Stage;

@Singleton
public class HelpSystemStage extends Stage {
	@Inject
	private HelpSystemStage(final StageManager stageManager, ResourceBundle bundle, final HelpSystem help) {
		this.setTitle(bundle.getString("helpsystem.stage.title"));
		this.setScene(new Scene(help));
		stageManager.register(this, this.getClass().getName());
		final File defaultPage = new File(help.getHelpSubdirectory(), "ProB2UI.html");
		help.openHelpPage(defaultPage, "");
	}
}
