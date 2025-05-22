package de.prob2.ui.helpsystem;

import java.util.ResourceBundle;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.internal.StageManager;

import javafx.scene.Scene;
import javafx.stage.Stage;

@Singleton
public final class HelpSystemStage extends Stage {
	@Inject
	private HelpSystemStage(final StageManager stageManager, ResourceBundle bundle, final HelpSystem help) {
		this.setTitle(bundle.getString("helpsystem.stage.title"));
		this.setScene(new Scene(help));
		this.setMinWidth(640);
		this.setMinHeight(480);
		stageManager.register(this, this.getClass().getName());
	}
}
