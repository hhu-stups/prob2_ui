package de.prob2.ui.menu;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.internal.StageManager;

import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

@Singleton
public class ReportBugStage extends Stage {
	@Inject
	public ReportBugStage(StageManager stageManager) {
		WebView webView = new WebView();
		WebEngine webEngine = webView.getEngine();
		webEngine.setJavaScriptEnabled(true);
		webEngine.load("https://probjira.atlassian.net/secure/RapidBoard.jspa?rapidView=8");
		
		this.setTitle("Report Bug");
		this.setScene(new Scene(webView));
		stageManager.register(this, this.getClass().getName());
	}
}
