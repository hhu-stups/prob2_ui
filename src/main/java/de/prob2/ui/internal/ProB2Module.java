package de.prob2.ui.internal;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.ResourceBundle;

import org.hildan.fxgson.FxGson;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.util.Providers;

import de.codecentric.centerdevice.MenuToolkit;
import de.prob.MainModule;
import de.prob2.ui.MainController;
import de.prob2.ui.animation.AnimationView;
import de.prob2.ui.animation.symbolic.SymbolicAnimationFormulaInput;
import de.prob2.ui.animation.symbolic.SymbolicAnimationView;
import de.prob2.ui.animation.tracereplay.TraceChecker;
import de.prob2.ui.animation.tracereplay.TraceReplayView;
import de.prob2.ui.beditor.BEditor;
import de.prob2.ui.beditor.BEditorView;
import de.prob2.ui.config.RuntimeOptions;
import de.prob2.ui.consoles.b.BConsole;
import de.prob2.ui.consoles.b.BConsoleView;
import de.prob2.ui.dynamic.DynamicCommandStatusBar;
import de.prob2.ui.dynamic.DynamicPreferencesTableView;
import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.history.HistoryView;
import de.prob2.ui.layout.FontSize;
import de.prob2.ui.menu.AdvancedMenu;
import de.prob2.ui.menu.FileMenu;
import de.prob2.ui.menu.HelpMenu;
import de.prob2.ui.menu.MainView;
import de.prob2.ui.menu.MenuController;
import de.prob2.ui.menu.ViewCodeStage;
import de.prob2.ui.menu.ViewMenu;
import de.prob2.ui.menu.VisualisationMenu;
import de.prob2.ui.menu.WindowMenu;
import de.prob2.ui.operations.ExecuteByPredicateStage;
import de.prob2.ui.operations.OperationsView;
import de.prob2.ui.plugin.ProBPluginHelper;
import de.prob2.ui.plugin.ProBPluginManager;
import de.prob2.ui.preferences.PreferencesView;
import de.prob2.ui.project.ProjectTab;
import de.prob2.ui.project.ProjectView;
import de.prob2.ui.project.machines.MachinesTab;
import de.prob2.ui.project.preferences.PreferencesTab;
import de.prob2.ui.project.verifications.MachineTableView;
import de.prob2.ui.project.verifications.VerificationsTab;
import de.prob2.ui.states.StateErrorsView;
import de.prob2.ui.states.StatesView;
import de.prob2.ui.stats.StatsView;
import de.prob2.ui.statusbar.StatusBar;
import de.prob2.ui.verifications.VerificationsView;
import de.prob2.ui.verifications.ltl.LTLView;
import de.prob2.ui.verifications.ltl.formula.LTLFormulaChecker;
import de.prob2.ui.verifications.modelchecking.ModelcheckingStage;
import de.prob2.ui.verifications.modelchecking.ModelcheckingView;
import de.prob2.ui.verifications.symbolicchecking.SymbolicCheckingFormulaInput;
import de.prob2.ui.verifications.symbolicchecking.SymbolicCheckingView;
import de.prob2.ui.visualisation.StateVisualisationView;
import de.prob2.ui.visualisation.VisualisationView;
import de.prob2.ui.visualisation.fx.VisualisationController;
import de.prob2.ui.visualisation.magiclayout.editPane.MagicLayoutEditEdges;
import de.prob2.ui.visualisation.magiclayout.editPane.MagicLayoutEditNodes;
import javafx.fxml.FXMLLoader;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.util.BuilderFactory;

public class ProB2Module extends AbstractModule {
	public static final boolean IS_MAC = System.getProperty("os.name", "").toLowerCase().contains("mac");

	private final RuntimeOptions runtimeOptions;
	private final BuilderFactory javafxDefaultBuilderFactory;

	public ProB2Module(final RuntimeOptions runtimeOptions) {
		super();
		this.runtimeOptions = runtimeOptions;
		this.javafxDefaultBuilderFactory = new JavaFXBuilderFactory();
	}

	@Override
	protected void configure() {
		install(new MainModule());

		// General stuff
		final Locale locale = Locale.getDefault();
		bind(Locale.class).toInstance(locale);
		final ResourceBundle bundle = ResourceBundle.getBundle("de.prob2.ui.prob2", locale);
		bind(ResourceBundle.class).toInstance(bundle);
		final MenuToolkit toolkit = IS_MAC && "true".equals(System.getProperty("de.prob2.ui.useMacMenuBar", "true"))
				? MenuToolkit.toolkit(locale)
				: null;
		bind(MenuToolkit.class).toProvider(Providers.of(toolkit));
		bind(RuntimeOptions.class).toInstance(this.runtimeOptions);
		bind(FontSize.class);
		bind(Gson.class).toInstance(FxGson.coreBuilder().disableHtmlEscaping().setPrettyPrinting()
				.registerTypeAdapter(File.class,
						(JsonSerializer<File>) (src, typeOfSrc, context) -> context.serialize(src.getPath()))
				.registerTypeAdapter(File.class,
						(JsonDeserializer<File>) (json, typeOfT, context) -> new File(json.getAsString()))
				.registerTypeAdapter(Path.class,
						(JsonSerializer<Path>) (src, typeOfSrc, context) -> context.serialize(src.toString()))
				.registerTypeAdapter(Path.class,
						(JsonDeserializer<Path>) (json, typeOfT, context) -> Paths.get(json.getAsString()))
				.create());

		// Controllers
		bind(AnimationView.class);
		bind(BConsole.class);
		bind(BConsoleView.class);
		bind(DynamicCommandStatusBar.class);
		bind(DynamicPreferencesTableView.class);
		bind(ExecuteByPredicateStage.class);
		bind(HistoryView.class);
		bind(BEditor.class);
		bind(BEditorView.class);
		bind(MainController.class);
		bind(LTLFormulaChecker.class);
		bind(HelpButton.class);
		bind(LTLView.class);
		bind(MachinesTab.class);
		bind(MainView.class);
		bind(MenuController.class);
		bind(FileMenu.class);
		bind(ViewMenu.class);
		bind(HelpMenu.class);
		bind(VisualisationMenu.class);
		bind(WindowMenu.class);
		bind(MachineTableView.class);
		bind(ModelcheckingView.class);
		bind(ModelcheckingStage.class);
		bind(NavigationButtons.class);
		bind(OperationsView.class);
		bind(PredicateBuilderView.class);
		bind(PreferencesTab.class);
		bind(PreferencesView.class);
		bind(ProjectView.class);
		bind(ProjectTab.class);
		bind(StateErrorsView.class);
		bind(StatesView.class);
		bind(StatsView.class);
		bind(StatusBar.class);
		bind(TraceReplayView.class);
		bind(VerificationsView.class);
		bind(VerificationsTab.class);
		bind(ViewCodeStage.class);
		bind(VisualisationView.class);
		bind(StateVisualisationView.class);
		bind(SymbolicCheckingView.class);
		bind(SymbolicCheckingFormulaInput.class);
		bind(SymbolicAnimationView.class);
		bind(SymbolicAnimationFormulaInput.class);
		bind(ProBPluginHelper.class);
		bind(ProBPluginManager.class);
		bind(VisualisationController.class);
		bind(AdvancedMenu.class);
		bind(TraceChecker.class);
		bind(MagicLayoutEditNodes.class);
		bind(MagicLayoutEditEdges.class);
	}

	@Provides
	public FXMLLoader provideLoader(final Injector injector, ResourceBundle bundle) {
		FXMLLoader fxmlLoader = new FXMLLoader();
		fxmlLoader.setBuilderFactory(type -> {
			if (injector.getExistingBinding(Key.get(type)) != null) {
				return () -> injector.getInstance(type);
			} else {
				return javafxDefaultBuilderFactory.getBuilder(type);
			}
		});
		fxmlLoader.setControllerFactory(injector::getInstance);
		fxmlLoader.setResources(bundle);
		return fxmlLoader;
	}
}
