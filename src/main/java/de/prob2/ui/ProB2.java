package de.prob2.ui;

import java.io.File;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;

import com.google.inject.Guice;
import com.google.inject.Injector;

import de.prob.cli.ProBInstanceProvider;
import de.prob.scripting.Api;

import de.prob2.ui.config.Config;
import de.prob2.ui.config.RuntimeOptions;
import de.prob2.ui.internal.ProB2Module;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.StopActions;
import de.prob2.ui.persistence.UIPersistence;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.ProjectManager;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.project.preferences.DefaultPreference;
import de.prob2.ui.project.preferences.Preference;
import de.prob2.ui.project.runconfigurations.Runconfiguration;
import de.prob2.ui.plugin.ProBPluginManager;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.application.Preloader;
import javafx.event.Event;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProB2 extends Application {
	private static final Logger LOGGER = LoggerFactory.getLogger(ProB2.class);

	private RuntimeOptions runtimeOptions;
	private Injector injector;
	private ResourceBundle bundle;

	private Stage primaryStage;

	public static void main(String... args) {
		Application.launch(args);
	}

	@Override
	public void init() {
		runtimeOptions = parseRuntimeOptions(this.getParameters().getRaw().toArray(new String[0]));
		if (runtimeOptions.isLoadConfig()) {
			final Locale localeOverride = Config.getLocaleOverride();
			if (localeOverride != null) {
				Locale.setDefault(localeOverride);
			}
		}
		ProB2Module module = new ProB2Module(runtimeOptions);
		Platform.runLater(() -> {
			injector = Guice.createInjector(com.google.inject.Stage.PRODUCTION, module);
			bundle = injector.getInstance(ResourceBundle.class);
			injector.getInstance(StopActions.class)
					.add(() -> injector.getInstance(ProBInstanceProvider.class).shutdownAll());
			StageManager stageManager = injector.getInstance(StageManager.class);
			Thread.setDefaultUncaughtExceptionHandler((thread, exc) -> {
				LOGGER.error("Uncaught exception on thread {}", thread, exc);
				Platform.runLater(() -> {
					final String message = String.format(
							"An internal exception occurred and was not caught. This is probably a bug.%nThread: %s",
							thread);
					final Alert alert = stageManager.makeExceptionAlert(Alert.AlertType.ERROR, message, exc);
					alert.setHeaderText("Uncaught internal exception");
					alert.show();
				});
			});
			LOGGER.info("probcli version: {}", injector.getInstance(Api.class).getVersion());
			// Load config file
			injector.getInstance(Config.class);

			CurrentProject currentProject = injector.getInstance(CurrentProject.class);
			currentProject.addListener((observable, from, to) -> this.updateTitle());
			currentProject.savedProperty().addListener((observable, from, to) -> this.updateTitle());
			CurrentTrace currentTrace = injector.getInstance(CurrentTrace.class);
			currentTrace.addListener((observable, from, to) -> this.updateTitle());
		});

	}

	@Override
	public void start(Stage primaryStage) {
		this.primaryStage = primaryStage;
		this.updateTitle();
		Parent root = injector.getInstance(MainController.class);
		Scene mainScene = new Scene(root, 1024, 768);
		primaryStage.setScene(mainScene);
		StageManager stageManager = injector.getInstance(StageManager.class);
		stageManager.registerMainStage(primaryStage, this.getClass().getName());

		CurrentProject currentProject = injector.getInstance(CurrentProject.class);
		primaryStage.setOnCloseRequest(event -> handleCloseRequest(event, currentProject, stageManager));

		this.notifyPreloader(new Preloader.ProgressNotification(100));
		this.primaryStage.show();

		UIPersistence uiPersistence = injector.getInstance(UIPersistence.class);
		uiPersistence.open();

		if (runtimeOptions.getProject() != null) {
			injector.getInstance(ProjectManager.class).openProject(new File(runtimeOptions.getProject()));
		}

		if (runtimeOptions.getMachine() != null) {
			final Machine foundMachine = currentProject.get().getMachine(runtimeOptions.getMachine());

			final Preference foundPreference;
			if (runtimeOptions.getPreference() == null) {
				foundPreference = new DefaultPreference();
			} else {
				foundPreference = currentProject.get().getPreference(runtimeOptions.getPreference());
			}

			if (foundMachine == null) {
				stageManager.makeAlert(
					Alert.AlertType.ERROR,
					String.format("There is no machine %s in project %s.", runtimeOptions.getMachine(), currentProject.getName())
				).show();
			} else if (foundPreference == null) {
				stageManager.makeAlert(
					Alert.AlertType.ERROR,
					String.format("There is no preference %s in project %s.", runtimeOptions.getPreference(), currentProject.getName())
				).show();
			} else {
				currentProject.startAnimation(new Runconfiguration(foundMachine, foundPreference));
			}
		}

		ProBPluginManager pluginManager = injector.getInstance(ProBPluginManager.class);
		pluginManager.start();
	}

	private void updateTitle() {
		final CurrentProject currentProject = injector.getInstance(CurrentProject.class);
		final CurrentTrace currentTrace = injector.getInstance(CurrentTrace.class);

		final StringBuilder title = new StringBuilder();

		if (currentProject.getCurrentRunconfiguration() != null) {
			title.append(currentProject.getCurrentRunconfiguration());
			if (currentTrace.exists()) {
				final File modelFile = currentTrace.getModel().getModelFile();
				if (modelFile != null) {
					title.append(" (");
					title.append(modelFile.getName());
					title.append(')');
				}
			}

			title.append(" - ");
		}

		if (currentProject.exists()) {
			title.append(currentProject.getName());
			title.append(" - ");
		}

		title.append("ProB 2.0");

		if (!currentProject.isSaved()) {
			title.append('*');
		}

		this.primaryStage.setTitle(title.toString());
	}

	private static IllegalStateException die(final String message, final int exitCode) {
		System.err.println(message);
		Platform.exit();
		System.exit(exitCode);
		return new IllegalStateException(message);
	}

	private static RuntimeOptions parseRuntimeOptions(final String[] args) {
		LOGGER.info("Parsing arguments: {}", (Object) args);

		final Options options = new Options();

		options.addOption(null, "project", true, "Open the specified project on startup.");
		options.addOption(null, "machine", true, "Load the specified machine from the project on startup. Requires --project.");
		options.addOption(null, "preference", true, "Use the specified preference set from the project when loading the machine. Requires --project and --machine.");
		options.addOption(null, "no-load-config", false, "Do not load the user config file, use the default config instead.");
		options.addOption(null, "no-save-config", false, "Do not save the user config file.");

		final CommandLineParser clParser = new PosixParser();
		final CommandLine cl;
		try {
			cl = clParser.parse(options, args);
		} catch (ParseException e) {
			LOGGER.error("Failed to parse command line", e);
			throw die(e.getLocalizedMessage(), 2);
		}
		LOGGER.info("Parsed command line: args {}, options {}", cl.getArgs(), cl.getOptions());

		if (!cl.getArgList().isEmpty()) {
			throw die("Positional arguments are not allowed: " + cl.getArgList(), 2);
		}

		if (cl.hasOption("machine") && !cl.hasOption("project")) {
			throw die("Invalid combination of options: --machine requires --project", 2);
		}

		if (cl.hasOption("preference") && !cl.hasOption("machine")) {
			throw die("Invalid combination of options: --preference requires --machine", 2);
		}

		final RuntimeOptions runtimeOpts = new RuntimeOptions(
			cl.getOptionValue("project"),
			cl.getOptionValue("machine"),
			cl.getOptionValue("preference"),
			!cl.hasOption("no-load-config"), 
			!cl.hasOption("no-save-config")
		);
		LOGGER.info("Created runtime options: {}", runtimeOpts);

		return runtimeOpts;
	}

	@Override
	public void stop() {
		if (injector != null) {
			injector.getInstance(StopActions.class).run();
		}
	}

	private void handleCloseRequest(Event event, CurrentProject currentProject, StageManager stageManager) {
		ProBPluginManager proBPluginManager = injector.getInstance(ProBPluginManager.class);
		proBPluginManager.getPluginManager().stopPlugins();
		if (!currentProject.isSaved()) {
			ButtonType save = new ButtonType(bundle.getString("common.save"), ButtonBar.ButtonData.YES);
			ButtonType doNotSave = new ButtonType(bundle.getString("common.doNotSave"), ButtonBar.ButtonData.NO);
			Alert alert = stageManager.makeAlert(
				Alert.AlertType.CONFIRMATION,
				String.format(bundle.getString("common.unsavedProjectChanges.message"), currentProject.getName()),
				save, ButtonType.CANCEL, doNotSave);
			Optional<ButtonType> result = alert.showAndWait();
			if (result.isPresent() && result.get().equals(ButtonType.CANCEL)) {
				event.consume();
			} else if (result.isPresent() && result.get().equals(save)) {
				injector.getInstance(ProjectManager.class).saveCurrentProject();
				Platform.exit();
			}
		} else {
			Platform.exit();
		}
	}
}
