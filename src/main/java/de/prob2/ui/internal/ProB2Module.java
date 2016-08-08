package de.prob2.ui.internal;

import java.util.Locale;
import java.util.ResourceBundle;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import de.prob.MainModule;
import de.prob2.ui.dotty.DottyView;
import de.prob2.ui.groovy.GroovyConsole;
import de.prob2.ui.history.HistoryView;
import de.prob2.ui.menu.MenuController;
import de.prob2.ui.modelchecking.ModelcheckingController;
import de.prob2.ui.modeline.ModelineController;
import de.prob2.ui.operations.OperationsView;
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
		
		// General stuff
		bind(ClassBlacklist.class);
		bind(ResourceBundle.class).toInstance(bundle);
		
		// Controllers
		bind(DottyView.class);
		bind(HistoryView.class);
		bind(MenuController.class);
		bind(ModelcheckingController.class);
		bind(ModelineController.class);
		bind(OperationsView.class);
		bind(StatesView.class);
		bind(GroovyConsole.class);
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
