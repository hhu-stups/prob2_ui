package de.prob2.ui;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class ProB2Preloader extends javafx.application.Preloader {

	Stage loadingStage;
	Scene scene;
	
	@Override
    public void init() {
		final Parent root = new BorderPane(new ImageView(ProB2.class.getResource("/prob_logo.gif").toExternalForm()));
		scene = new Scene(root);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		this.loadingStage = primaryStage;
		this.loadingStage.setTitle("Loading ProB 2.0...");
		this.loadingStage.setScene(scene);
		this.loadingStage.show();
	}
	
	@Override
	public void handleProgressNotification(ProgressNotification info) {
		if (info.getProgress() == 100) {
			loadingStage.hide();
		}
	}
}
