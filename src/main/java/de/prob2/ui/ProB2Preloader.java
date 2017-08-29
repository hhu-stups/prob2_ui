package de.prob2.ui;

import javafx.application.Preloader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class ProB2Preloader extends Preloader {
	private Stage loadingStage;
	private Scene scene;
	
	@Override
	public void init() {
		final Parent root = new BorderPane(new ImageView(ProB2Preloader.class.getResource("/prob_logo.gif").toExternalForm()));
		scene = new Scene(root);
	}
	
	@Override
	public void start(Stage primaryStage) {
		this.loadingStage = primaryStage;
		this.loadingStage.setTitle("Loading ProB 2.0...");
		this.loadingStage.setScene(scene);
		this.loadingStage.show();
	}
	
	@Override
	public void handleApplicationNotification(final Preloader.PreloaderNotification info) {
		if (info instanceof Preloader.ProgressNotification && ((Preloader.ProgressNotification)info).getProgress() == 100) {
			this.loadingStage.hide();
		}
	}
}
