package de.prob2.ui.helpsystem;

import java.io.File;
import java.util.ResourceBundle;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.Main;
import de.prob2.ui.ProB2;
import de.prob2.ui.internal.StageManager;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

@Singleton
public class HelpSystemStage extends Stage {
	@Inject
	private HelpSystemStage(final StageManager stageManager, ResourceBundle bundle, final HelpSystem help) {
		this.setTitle(bundle.getString("helpsystem.stage.title"));
		this.setScene(new Scene(help));
		stageManager.register(this, this.getClass().getName());
		String defaultDir;
		if (help.isJar)
			defaultDir = Main.getProBDirectory() + "prob2ui" + File.separator + "help" + File.separator + help.helpSubdirectoryString + File.separator;
		else
			defaultDir = ProB2.class.getClassLoader().getResource("help" + File.separator + help.helpSubdirectoryString + File.separator).toString();
		File defaultPage = new File(defaultDir + "ProB2UI.html");
		setContent(defaultPage,"");
	}

	public void setContent(File file, String anchor) {
		String uri = file.toURI().toString();
		int lastIndex = uri.lastIndexOf("file:/");
		Platform.runLater(() ->	((HelpSystem) this.getScene().getRoot()).webEngine.load(uri.substring(lastIndex).replace("%25","%") + anchor));
	}
}
