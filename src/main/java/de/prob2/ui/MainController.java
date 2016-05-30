package de.prob2.ui;

import java.io.IOException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.ResourceBundle;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Injector;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.scene.Parent;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.util.BuilderFactory;
import javafx.util.Callback;

public class MainController implements Initializable {

	@FXML
	BorderPane rootContainer;

	private final MenuBar menuBar;
	private final LinkedHashMap<String, Menu> menus = new LinkedHashMap<>();

	private FXMLLoader loader;

	@Inject
	public MainController(MenuBar menuBar, FXMLLoader loader) {
		this.menuBar = menuBar;
		this.loader = loader;
		final String os = System.getProperty("os.name");
		if (os != null && os.startsWith("Mac"))
			menuBar.useSystemMenuBarProperty().set(true);
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		configureMenus();
		addMenus();
		// fillMenus();
		rootContainer.setTop(menuBar);

		BuilderFactory builderFactory = new JavaFXBuilderFactory();
		Callback<Class<?>, Object> guiceControllerFactory = clazz -> ProB2.injector.getInstance(clazz);

		try {
			Parent animantion_root = new FXMLLoader(getClass().getResource("animation_perspective.fxml"), null,
					builderFactory, guiceControllerFactory).load();
			Parent modeline = new FXMLLoader(getClass().getResource("modeline/modeline.fxml"), null,
					new JavaFXBuilderFactory(), guiceControllerFactory).load();
			StackPane stack = new StackPane();
			stack.getChildren().addAll(animantion_root, modeline);
			rootContainer.setCenter(stack);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}



	}

	private void addMenus() {
		Set<String> keySet = menus.keySet();
		for (String string : keySet) {
			System.out.println(string);
		}
	}

	private void configureMenus() {
		createMenu("File");
		createMenu("Edit");
		createMenu("Help");
	}

	private void createMenu(String string) {
		menus.put(string, new Menu(string));
	}

}
