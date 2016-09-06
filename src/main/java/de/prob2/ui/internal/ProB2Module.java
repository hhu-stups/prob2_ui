package de.prob2.ui.internal;

import java.util.Locale;
import java.util.ResourceBundle;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import de.prob.MainModule;
import de.prob2.ui.animations.AnimationsView;
import de.prob2.ui.formula.FormulaGenerator;
import de.prob2.ui.history.HistoryView;
import de.prob2.ui.menu.MenuController;
import de.prob2.ui.modelchecking.ModelcheckingController;
import de.prob2.ui.modeline.ModelineController;
import de.prob2.ui.operations.OperationsView;
import de.prob2.ui.prob2fx.CurrentModel;
import de.prob2.ui.prob2fx.CurrentState;
import de.prob2.ui.prob2fx.CurrentStateSpace;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.states.ClassBlacklist;
import de.prob2.ui.states.StatesView;
import javafx.fxml.FXMLLoader;

public class ProB2Module extends AbstractModule {

	private final Locale locale = new Locale("en");
	private final ResourceBundle bundle = ResourceBundle.getBundle("bundles.prob2", locale);

	@Override
	protected void configure() {
		install(new MainModule());
		
		// General stuff
		bind(ClassBlacklist.class);
		bind(CurrentModel.class);
		bind(CurrentState.class);
		bind(CurrentStateSpace.class);
		bind(CurrentTrace.class);
		bind(FormulaGenerator.class);
		bind(ResourceBundle.class).toInstance(bundle);

		// Controllers
		bind(HistoryView.class);
		bind(MenuController.class);
		bind(ModelcheckingController.class);
		bind(ModelineController.class);
		bind(OperationsView.class);
		bind(StatesView.class);
		bind(AnimationsView.class);
	}

	@Provides
	public FXMLLoader provideLoader(final Injector injector, GuiceBuilderFactory builderFactory,
			ResourceBundle bundle) {

		FXMLLoader fxmlLoader = new FXMLLoader();
		fxmlLoader.setBuilderFactory(builderFactory);
		fxmlLoader.setControllerFactory(injector::getInstance);
		fxmlLoader.setResources(bundle);
		return fxmlLoader;
	}
}
