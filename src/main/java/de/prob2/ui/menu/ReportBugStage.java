package de.prob2.ui.menu;

import java.util.ResourceBundle;

import com.google.inject.Inject;
import de.prob2.ui.internal.StageManager;
import javafx.scene.Scene;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

public class ReportBugStage extends Stage {
	@Inject
	public ReportBugStage(StageManager stageManager, ResourceBundle bundle) {
		WebView webView = new WebView();
		String script = getClass().getResource("bugreportbutton.html").toExternalForm();
		webView.getEngine().load(script);
		this.setTitle(bundle.getString("menu.reportBug.stage.title"));
		this.setScene(new Scene(webView));
		this.setMinWidth(640);
		this.setMinHeight(480);
		this.initOwner(stageManager.getMainStage());
		stageManager.register(this, this.getClass().getName());
	}
}
