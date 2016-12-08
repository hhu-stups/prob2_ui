package de.prob2.ui.internal;

import java.util.Locale;
import java.util.ResourceBundle;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;

import de.prob.MainModule;

import de.prob2.ui.AnimationPerspective;
import de.prob2.ui.MainController;
import de.prob2.ui.animations.AnimationsView;
import de.prob2.ui.config.Config;
import de.prob2.ui.consoles.b.BConsole;
import de.prob2.ui.consoles.groovy.GroovyConsole;
import de.prob2.ui.formula.FormulaGenerator;
import de.prob2.ui.formula.FormulaInputStage;
import de.prob2.ui.history.HistoryView;
import de.prob2.ui.menu.MenuController;
import de.prob2.ui.menu.RecentFiles;
import de.prob2.ui.modelchecking.ModelcheckingController;
import de.prob2.ui.operations.OperationsView;
import de.prob2.ui.prob2fx.CurrentModel;
import de.prob2.ui.prob2fx.CurrentState;
import de.prob2.ui.prob2fx.CurrentStateSpace;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.states.ClassBlacklist;
import de.prob2.ui.states.StatesView;
import de.prob2.ui.stats.StatsView;

import javafx.fxml.FXMLLoader;

public class ProB2Module extends AbstractModule {

	private final Locale locale = new Locale("en");
	private final ResourceBundle bundle = ResourceBundle.getBundle("bundles.prob2", locale);

	@Override
	protected void configure() {
		install(new MainModule());
		
		// General stuff
		bind(ClassBlacklist.class);
		bind(Config.class);
		bind(CurrentModel.class);
		bind(CurrentState.class);
		bind(CurrentStateSpace.class);
		bind(CurrentTrace.class);
		bind(FormulaGenerator.class);
		bind(FormulaInputStage.class);
		bind(GroovyConsole.class);
		bind(BConsole.class);
		bind(RecentFiles.class);
		bind(ResourceBundle.class).toInstance(bundle);

		// Controllers
		bind(AnimationPerspective.class);
		bind(AnimationsView.class);
		bind(HistoryView.class);
		bind(MenuController.class);
		bind(MainController.class);
		bind(ModelcheckingController.class);
		bind(OperationsView.class);
		bind(StatesView.class);
		bind(StatsView.class);
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
