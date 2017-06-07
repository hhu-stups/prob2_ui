package de.prob2.ui.internal;

import java.util.Locale;
import java.util.ResourceBundle;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.util.Providers;

import de.codecentric.centerdevice.MenuToolkit;

import de.prob.MainModule;
import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.verifications.ltl.LTLChecker;
import de.prob2.ui.MainController;
import de.prob2.ui.bmotion.BMotionView;
import de.prob2.ui.history.HistoryView;
import de.prob2.ui.menu.MainView;
import de.prob2.ui.menu.MenuController;
import de.prob2.ui.operations.OperationsView;
import de.prob2.ui.preferences.PreferencesView;
import de.prob2.ui.project.ProjectTab;
import de.prob2.ui.project.ProjectView;
import de.prob2.ui.project.machines.MachinesTab;
import de.prob2.ui.project.preferences.PreferencesTab;
import de.prob2.ui.project.runconfigurations.RunconfigurationsTab;
import de.prob2.ui.states.StatesView;
import de.prob2.ui.stats.StatsView;
import de.prob2.ui.statusbar.StatusBar;
import de.prob2.ui.verifications.VerificationsView;
import de.prob2.ui.verifications.ltl.LTLView;
import de.prob2.ui.verifications.ltl.MachineTableView;
import de.prob2.ui.verifications.modelchecking.ModelcheckingController;

import javafx.fxml.FXMLLoader;

public class ProB2Module extends AbstractModule {
	public static final boolean IS_MAC = System.getProperty("os.name", "").toLowerCase().contains("mac");
	
	private final Locale locale = new Locale("en");
	private final ResourceBundle bundle = ResourceBundle.getBundle("bundles.prob2", locale);

	@Override
	protected void configure() {
		install(new MainModule());
		
		// General stuff
		bind(Locale.class).toInstance(locale);
		bind(ResourceBundle.class).toInstance(bundle);
		if (IS_MAC) {
			bind(MenuToolkit.class).toInstance(MenuToolkit.toolkit(locale));
		} else {
			bind(MenuToolkit.class).toProvider(Providers.of(null));
		}
		
		// Controllers
		bind(BMotionView.class);
		bind(HistoryView.class);
		bind(HelpButton.class);
		bind(LTLChecker.class);
		bind(MachineTableView.class);
		bind(LTLView.class);
		bind(MachinesTab.class);
		bind(MainView.class);
		bind(MainController.class);
		bind(MenuController.class);
		bind(ModelcheckingController.class);
		bind(OperationsView.class);
		bind(PreferencesTab.class);
		bind(PreferencesView.class);
		bind(ProjectView.class);
		bind(ProjectTab.class);
		bind(RunconfigurationsTab.class);
		bind(StatesView.class);
		bind(StatsView.class);
		bind(StatusBar.class);
		bind(VerificationsView.class);
	}

	@Provides
	public FXMLLoader provideLoader(final Injector injector, GuiceBuilderFactory builderFactory, ResourceBundle bundle) { 
		FXMLLoader fxmlLoader = new FXMLLoader();
		fxmlLoader.setBuilderFactory(builderFactory);
		fxmlLoader.setControllerFactory(injector::getInstance);
		fxmlLoader.setResources(bundle);
		return fxmlLoader;
	}
}
