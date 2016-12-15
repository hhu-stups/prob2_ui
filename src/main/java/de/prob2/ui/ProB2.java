package de.prob2.ui;

import com.google.inject.Guice;
import com.google.inject.Injector;

import de.prob.cli.ProBInstanceProvider;
import de.prob2.ui.config.Config;
import de.prob2.ui.internal.ProB2Module;
import de.prob2.ui.internal.UIPersistence;
import de.prob2.ui.internal.UIState;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentStage;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.BoundingBox;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class ProB2 extends Application {
	private Injector injector;
	private Config config;
	
	public static void main(String... args) {
		launch(args);
	}

	@Override
	public void start(Stage stage) throws Exception {
		ProB2Module module = new ProB2Module();
		injector = Guice.createInjector(com.google.inject.Stage.PRODUCTION, module);
		config = injector.getInstance(Config.class);
		UIPersistence uiPersistence = injector.getInstance(UIPersistence.class);
		UIState uiState = injector.getInstance(UIState.class);
		
		Parent root = injector.getInstance(MainController.class);
		Scene mainScene = new Scene(root);
		mainScene.getStylesheets().add("prob.css");
		stage.setTitle("ProB 2.0");
		stage.setScene(mainScene);
		stage.getIcons().add(new Image("prob_128.gif"));
		stage.setOnCloseRequest(e -> Platform.exit());

		CurrentProject currentProject = injector.getInstance(CurrentProject.class);
		currentProject.addListener((observable, from, to) -> stage.setTitle("ProB 2.0 [" + to.getName() + "]"));
		
		injector.getInstance(CurrentStage.class).register(stage, this.getClass().getName());
		
		final BoundingBox mainBox = uiState.getSavedStageBoxes().get(this.getClass().getName());
		if (mainBox != null) {
			stage.setX(mainBox.getMinX());
			stage.setY(mainBox.getMinY());
			stage.setWidth(mainBox.getWidth());
			stage.setHeight(mainBox.getHeight());
		}
		stage.show();
		uiPersistence.open();
	}
	
	@Override
	public void stop() {
		config.save();
		injector.getInstance(ProBInstanceProvider.class).shutdownAll();
	}

}
