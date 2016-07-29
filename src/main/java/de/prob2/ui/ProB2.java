package de.prob2.ui;

import com.google.inject.Guice;
import com.google.inject.Injector;

import de.prob2.ui.internal.ProB2Module;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ProB2 extends Application {

	public static Injector injector;

	public static void main(String... args) {

		Platform.setImplicitExit(true);
		launch(args);
	}

	@Override
	public void start(Stage stage) throws Exception {

		ProB2Module module = new ProB2Module();
		this.injector = Guice.createInjector(com.google.inject.Stage.PRODUCTION, module);
		// menuBar = injector.getInstance(MenuBar.class);

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

		// configureMenus();
		// addMenus();

		stage.show();
	}

	// private void addMenus() {
	// Collection<Menu> values = menus.values();
	// menuBar.getMenus().addAll(values);
	// }
	//
	// private void configureMenus() {
	// createFileMenu();
	// createMenu("Edit");
	// createMenu("Help");
	// }
	//
	// private void createFileMenu() {
	// Menu file = new Menu("File");
	// file.getItems().add(new MenuItem("Exit"));
	// menus.put("File", file);
	// }
	//
	// private void createMenu(String string) {
	// menus.put(string, new Menu(string));
	// }

}
