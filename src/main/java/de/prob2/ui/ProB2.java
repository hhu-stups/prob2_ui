package de.prob2.ui;

import com.google.inject.Guice;
import com.google.inject.Injector;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.BuilderFactory;
import javafx.util.Callback;

public class ProB2 extends Application {

	public static final Injector injector = Guice.createInjector(com.google.inject.Stage.PRODUCTION, new ProB2Module());

	public static void main(String... args) {
		Platform.setImplicitExit(true);
		launch(args);
	}

	@Override
	public void start(Stage stage) throws Exception {

		Callback<Class<?>, Object> guiceControllerFactory = clazz -> injector.getInstance(clazz);
		BuilderFactory builderFactory = injector.getInstance(BFF.class);

		Parent main_scene = new FXMLLoader(getClass().getResource("main.fxml"), null, builderFactory,
				guiceControllerFactory).load();

		Scene mainScene = new Scene(main_scene, 1024, 768);

		stage.setTitle("ProB 2.0");
		stage.setScene(mainScene);

		stage.show();

	}

}
