package de.prob2.ui.helpsystem;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ResourceBundle;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.Main;

import de.prob2.ui.internal.StageManager;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.stage.Stage;

@Singleton
public class HelpSystemStage extends Stage {
	HelpSystem help;

	@Inject
	private HelpSystemStage(final StageManager stageManager, ResourceBundle bundle, final Injector injector) throws URISyntaxException, IOException{
		this.setTitle(bundle.getString("helpsystem.stage.title"));
		help = new HelpSystem(stageManager, injector);
		this.setScene(new Scene(help));
		stageManager.register(this, this.getClass().getName());
		setContent(new File(Main.getProBDirectory() + "prob2ui" + File.separator + "help" + File.separator + help.helpSubdirectoryString + File.separator+ "ProB2UI.md.html"),"");
	}

	public void setContent(File file, String anchor) {
		Platform.runLater(() ->	((HelpSystem) this.getScene().getRoot()).webEngine.load(file.toURI().toString() + anchor));
	}
}
