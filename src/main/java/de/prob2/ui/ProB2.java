package de.prob2.ui;

import com.google.inject.Guice;
import com.google.inject.Injector;

import de.prob.cli.ProBInstanceProvider;

import de.prob2.ui.config.Config;
import de.prob2.ui.internal.ProB2Module;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.persistence.UIPersistence;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ProB2 extends Application {
	private Injector injector;
	private Config config;
	
	private Stage primaryStage;

	public static void main(String... args) {
		launch(args);
	}
	
	private void updateTitle() {
		final CurrentProject currentProject = injector.getInstance(CurrentProject.class);
		final CurrentTrace currentTrace = injector.getInstance(CurrentTrace.class);
		
		final StringBuilder title = new StringBuilder();
		if (currentTrace.exists()) {
			title.append(currentTrace.getModel().getModelFile().getName());
			title.append(" - ");
		}
		if (currentProject.exists()) {
			title.append(currentProject.getName());
			title.append(" - ");
		}
		title.append("ProB 2.0");
		
		this.primaryStage.setTitle(title.toString());
	}

	@Override
	public void start(Stage primaryStage) {
		this.primaryStage = primaryStage;
		ProB2Module module = new ProB2Module();
		injector = Guice.createInjector(com.google.inject.Stage.PRODUCTION, module);
		config = injector.getInstance(Config.class);
		UIPersistence uiPersistence = injector.getInstance(UIPersistence.class);
		Parent root = injector.getInstance(MainController.class);
		Scene mainScene = new Scene(root, 1024, 768);
		primaryStage.setScene(mainScene);
		primaryStage.setOnCloseRequest(e -> Platform.exit());

		CurrentProject currentProject = injector.getInstance(CurrentProject.class);
		currentProject.addListener((observable, from, to) -> this.updateTitle());
		CurrentTrace currentTrace = injector.getInstance(CurrentTrace.class);
		currentTrace.addListener((observable, from, to) -> this.updateTitle());
		this.updateTitle();

		injector.getInstance(StageManager.class).register(primaryStage, this.getClass().getName());

		primaryStage.show();
		uiPersistence.open();
	}

	@Override
	public void stop() {
		config.save();
		injector.getInstance(ProBInstanceProvider.class).shutdownAll();
	}
}
