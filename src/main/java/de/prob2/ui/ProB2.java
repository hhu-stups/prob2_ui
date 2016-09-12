package de.prob2.ui;

import com.google.inject.Guice;
import com.google.inject.Injector;

import de.prob2.ui.internal.ProB2Module;
import de.prob2.ui.prob2fx.CurrentStage;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ProB2 extends Application {
	public static void main(String... args) {
		Platform.setImplicitExit(true);
		launch(args);
	}

	@Override
	public void start(Stage stage) throws Exception {
		ProB2Module module = new ProB2Module();
		Injector injector = Guice.createInjector(com.google.inject.Stage.PRODUCTION, module);

		FXMLLoader loader = injector.getInstance(FXMLLoader.class);
		loader.setLocation(getClass().getResource("main.fxml"));

		loader.load();

		Parent root = loader.getRoot();

		Scene mainScene = new Scene(root, 1024, 768);
		mainScene.getStylesheets().add("prob.css");
		stage.setTitle("ProB 2.0");
		stage.setScene(mainScene);

		stage.setOnCloseRequest(e -> {
			Platform.exit();
			System.exit(0);
		});
		
		injector.getInstance(CurrentStage.class).register(stage);

		stage.show();
	}
}
