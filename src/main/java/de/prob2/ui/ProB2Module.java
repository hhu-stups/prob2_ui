package de.prob2.ui;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;

import de.prob.MainModule;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.MenuBar;
import javafx.util.Callback;

public class ProB2Module extends AbstractModule {

	@Override
	protected void configure() {
		bind(MenuBar.class).asEagerSingleton();
		install(new MainModule());
		bind(Database.class).toInstance(new Database());
	}

	@Provides
	public FXMLLoader provideLoader(Injector injector, BFF builderFactory) {
		Callback<Class<?>, Object> guiceControllerFactory = clazz -> injector.getInstance(clazz);
		FXMLLoader fxmlLoader = new FXMLLoader();
		fxmlLoader.setBuilderFactory(builderFactory);
		fxmlLoader.setControllerFactory(guiceControllerFactory);
		return fxmlLoader;
	}

}
