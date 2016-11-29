package de.prob2.ui;

import com.google.inject.Guice;
import com.google.inject.Injector;

import de.prob.cli.ProBInstanceProvider;
import de.prob2.ui.config.Config;
import de.prob2.ui.internal.ProB2Module;
import de.prob2.ui.internal.UIPersistence;
import de.prob2.ui.internal.UIState;
import de.prob2.ui.prob2fx.CurrentStage;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class ProB2 extends Application {
	private Injector injector;
	private Config config;
	private UIPersistence uiPersistence;
	
	public static void main(String... args) {
		launch(args);
	}

	@Override
	public void start(Stage stage) throws Exception {
		ProB2Module module = new ProB2Module();
		injector = Guice.createInjector(com.google.inject.Stage.PRODUCTION, module);
		config = injector.getInstance(Config.class);
		UIState uiState = injector.getInstance(UIState.class);
		uiPersistence = new UIPersistence(uiState, injector);
		FXMLLoader loader = injector.getInstance(FXMLLoader.class);
		loader.setLocation(getClass().getResource("main.fxml"));
		loader.load();
		Parent root = loader.getRoot();

		Scene mainScene = new Scene(root, 1024, 768);
		mainScene.getStylesheets().add("prob.css");
		stage.setTitle("ProB 2.0");
		stage.setScene(mainScene);
		stage.getIcons().add(new Image("prob_128.gif"));
		stage.setOnCloseRequest(e -> Platform.exit());
		
		injector.getInstance(CurrentStage.class).register(stage);
		uiPersistence.open();
		stage.show();
		
	}
	
	@Override
	public void stop() {
		config.save();
		injector.getInstance(ProBInstanceProvider.class).shutdownAll();
	}

}
