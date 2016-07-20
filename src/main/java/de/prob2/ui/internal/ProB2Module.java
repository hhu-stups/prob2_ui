package de.prob2.ui.internal;

import java.util.Locale;
import java.util.ResourceBundle;

import com.google.common.eventbus.EventBus;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import de.prob.MainModule;
import de.prob2.ui.commands.OpenFileCommand;
import de.prob2.ui.menu.MenuController;
import de.prob2.ui.modelchecking.ModelcheckingController;
import de.prob2.ui.modeline.ModelineController;
import de.prob2.ui.states.ClassBlacklist;
import de.prob2.ui.states.StatesView;
import javafx.fxml.FXMLLoader;
import javafx.util.Callback;

public class ProB2Module extends AbstractModule {

	private Locale locale = new Locale("en");
	private ResourceBundle bundle = ResourceBundle.getBundle("bundles.prob2", locale);

	@Override
	protected void configure() {
		install(new MainModule());
		
		bind(EventBus.class).asEagerSingleton();
		bind(MenuController.class);
		bind(ModelineController.class);
		bind(ClassBlacklist.class);
		bind(StatesView.class);
		bind(ResourceBundle.class).toInstance(bundle);
		bind(OpenFileCommand.class).asEagerSingleton();
		bind(ModelcheckingController.class).asEagerSingleton();
	}

	@Provides
	public FXMLLoader provideLoader(final Injector injector, GuiceBuilderFactory builderFactory,
			ResourceBundle bundle) {

		Callback<Class<?>, Object> guiceControllerFactory = type -> injector.getInstance(type);

		FXMLLoader fxmlLoader = new FXMLLoader();
		fxmlLoader.setBuilderFactory(builderFactory);
		fxmlLoader.setControllerFactory(guiceControllerFactory);
		fxmlLoader.setResources(bundle);
		return fxmlLoader;
	}

}
