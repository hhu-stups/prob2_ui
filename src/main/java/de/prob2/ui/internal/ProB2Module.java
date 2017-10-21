package de.prob2.ui.internal;

import java.util.Locale;
import java.util.ResourceBundle;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.util.Providers;

import de.codecentric.centerdevice.MenuToolkit;

import de.prob.MainModule;

import de.prob2.ui.MainController;
import de.prob2.ui.config.RuntimeOptions;
import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.history.HistoryView;
import de.prob2.ui.menu.*;
import de.prob2.ui.operations.OperationsView;
import de.prob2.ui.plugin.ProBConnection;
import de.prob2.ui.plugin.ProBPluginManager;
import de.prob2.ui.preferences.PreferencesView;
import de.prob2.ui.project.ProjectTab;
import de.prob2.ui.project.ProjectView;
import de.prob2.ui.project.machines.MachinesTab;
import de.prob2.ui.project.preferences.PreferencesTab;
import de.prob2.ui.project.runconfigurations.RunconfigurationsTab;
import de.prob2.ui.project.verifications.MachineTableView;
import de.prob2.ui.project.verifications.VerificationsTab;
import de.prob2.ui.states.StateErrorsView;
import de.prob2.ui.states.StatesView;
import de.prob2.ui.stats.StatsView;
import de.prob2.ui.statusbar.StatusBar;
import de.prob2.ui.verifications.VerificationsView;
import de.prob2.ui.verifications.ltl.LTLView;
import de.prob2.ui.verifications.ltl.formula.LTLFormulaChecker;
import de.prob2.ui.verifications.modelchecking.ModelcheckingController;
import de.prob2.ui.verifications.symbolicchecking.SymbolicCheckingFormulaInput;
import de.prob2.ui.verifications.symbolicchecking.SymbolicCheckingView;
import de.prob2.ui.visualisation.StateVisualisationView;
import de.prob2.ui.visualisation.VisualisationView;

import de.prob2.ui.visualisation.fx.VisualisationController;
import javafx.fxml.FXMLLoader;

public class ProB2Module extends AbstractModule {
	public static final boolean IS_MAC = System.getProperty("os.name", "").toLowerCase().contains("mac");
	
	private final RuntimeOptions runtimeOptions;
	
	public ProB2Module(final RuntimeOptions runtimeOptions) {
		super();
		this.runtimeOptions = runtimeOptions;
	}

	@Override
	protected void configure() {
		install(new MainModule());
		
		// General stuff
		final Locale locale = Locale.getDefault();
		bind(Locale.class).toInstance(locale);
		final ResourceBundle bundle = ResourceBundle.getBundle("de.prob2.ui.prob2", locale);
		bind(ResourceBundle.class).toInstance(bundle);
		final MenuToolkit toolkit = IS_MAC ? MenuToolkit.toolkit(locale) : null;
		bind(MenuToolkit.class).toProvider(Providers.of(toolkit));
		bind(RuntimeOptions.class).toInstance(this.runtimeOptions);
		
		// Controllers
		bind(HistoryView.class);
		bind(MainController.class);
		bind(LTLFormulaChecker.class);
		bind(HelpButton.class);
		bind(LTLView.class);
		bind(MachinesTab.class);
		bind(MainView.class);
		bind(MenuController.class);
		bind(FileMenu.class);
		bind(EditMenu.class);
		bind(FormulaMenu.class);
		bind(ConsolesMenu.class);
		bind(PerspectivesMenu.class);
		bind(ViewMenu.class);
		bind(PluginMenu.class);
		bind(HelpMenu.class);
		bind(MachineTableView.class);
		bind(ModelcheckingController.class);
		bind(OperationsView.class);
		bind(PreferencesTab.class);
		bind(PreferencesView.class);
		bind(ProjectView.class);
		bind(ProjectTab.class);
		bind(RunconfigurationsTab.class);
		bind(StateErrorsView.class);
		bind(StatesView.class);
		bind(StatsView.class);
		bind(StatusBar.class);
		bind(VerificationsView.class);
		bind(VerificationsTab.class);
		bind(VisualisationView.class);
		bind(StateVisualisationView.class);
        bind(SymbolicCheckingView.class);
        bind(SymbolicCheckingFormulaInput.class);

		bind(ProBPluginManager.class);
		bind(ProBConnection.class);

		bind(VisualisationController.class);
		bind(VisualisationMenu.class);

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
