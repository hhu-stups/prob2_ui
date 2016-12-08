package de.prob2.ui.menu;



import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.prob2fx.CurrentStage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

@Singleton
public class ReportBugStage extends Stage {
		
	@Inject
	public ReportBugStage(FXMLLoader loader, CurrentStage currentStage) {
		WebView webView = new WebView();
		WebEngine webEnging = webView.getEngine();
		webEnging.setJavaScriptEnabled(true);
		webEnging.load("https://probjira.atlassian.net/secure/RapidBoard.jspa?rapidView=8");
		
		Scene scene = new Scene(webView);
		scene.getStylesheets().add("prob.css");
		
		this.setTitle("Report Bug");
		this.setScene(scene);
		currentStage.register(this);
	}
	
}
