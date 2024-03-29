package de.prob2.ui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;

import de.jangassen.MenuToolkit;
import de.prob.cli.ProBInstanceProvider;
import de.prob2.ui.config.BasicConfig;
import de.prob2.ui.config.ConfigData;
import de.prob2.ui.config.RuntimeOptions;
import de.prob2.ui.error.ExceptionAlert;
import de.prob2.ui.internal.BasicConfigModule;
import de.prob2.ui.internal.ConfigDirectory;
import de.prob2.ui.internal.ConfigFile;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.ProB2Module;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.StopActions;
import de.prob2.ui.persistence.UIPersistence;
import de.prob2.ui.plugin.ProBPluginManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.MachineLoader;
import de.prob2.ui.project.Project;
import de.prob2.ui.project.ProjectManager;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.project.preferences.Preference;
import de.prob2.ui.simulation.simulators.RealTimeSimulator;
import de.prob2.ui.simulation.simulators.Scheduler;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.ClassicConstants;

public class ProB2 extends Application {

	public static final String BUG_REPORT_URL = "https://github.com/hhu-stups/prob-issues/issues/new/choose";

	// This logger needs to be non-static,
	// so that the init method can set the custom logging config before creating the logger.
	// The logging config won't take effect if any logger is created before the config property is set.
	private Logger logger;

	private RuntimeOptions runtimeOptions;
	private Injector injector;
	private I18n i18n;
	private StopActions stopActions;

	private static IllegalStateException die(final String message, final int exitCode) {
		if (message != null) {
			System.err.println(message);
		}

		Platform.exit();
		System.exit(exitCode);
		return new IllegalStateException(message);
	}

	private void migrateOldConfigFileIfNeeded(final Injector basicConfigInjector) throws IOException {
		final Path currentConfigFilePath = basicConfigInjector.getInstance(Key.get(Path.class, ConfigFile.class));
		// Attempt migration only if no current config file exists.
		if (!Files.exists(currentConfigFilePath)) {
			final Path configDirectory = basicConfigInjector.getInstance(Key.get(Path.class, ConfigDirectory.class));
			Files.createDirectories(configDirectory);
			Path foundPreviousConfigFilePath = null;
			// Search for the newest versioned config file that exists.
			for (int version = ConfigData.CURRENT_FORMAT_VERSION - 1; version >= 2; version--) {
				final Path previousConfigFilePath = configDirectory.resolve(ConfigData.configFileNameForVersion(version));
				if (Files.exists(previousConfigFilePath)) {
					foundPreviousConfigFilePath = previousConfigFilePath;
					break;
				}
			}
			if (foundPreviousConfigFilePath == null) {
				// No versioned config file found - look for an unversioned config file
				// (format version 1 or 2, from ProB 2 UI 1.1.0 or later snapshots of that version).
				final Path unversionedConfigFilePath = configDirectory.resolve("config.json");
				if (Files.exists(unversionedConfigFilePath)) {
					foundPreviousConfigFilePath = unversionedConfigFilePath;
				}
			}
			if (foundPreviousConfigFilePath != null) {
				logger.info("Found old config file at {} - migrating to {}", foundPreviousConfigFilePath, currentConfigFilePath);
				Files.copy(foundPreviousConfigFilePath, currentConfigFilePath);
			}
		}
	}

	@Override
	public void init() {
		if (!System.getProperties().containsKey(ClassicConstants.CONFIG_FILE_PROPERTY)) {
			System.setProperty(ClassicConstants.CONFIG_FILE_PROPERTY, "de/prob2/ui/logback_config.xml");
		}
		logger = LoggerFactory.getLogger(ProB2.class);

		runtimeOptions = parseRuntimeOptions(this.getParameters().getRaw().toArray(new String[0]));
		if (runtimeOptions.isLoadConfig()) {
			final Injector basicConfigInjector = Guice.createInjector(new BasicConfigModule());
			try {
				this.migrateOldConfigFileIfNeeded(basicConfigInjector);
			} catch (IOException e) {
				logger.error("Failed to migrate old config - ignoring", e);
			}
			final BasicConfig basicConfig = basicConfigInjector.getInstance(BasicConfig.class);
			final Locale localeOverride = basicConfig.getLocaleOverride();
			if (localeOverride != null) {
				Locale.setDefault(localeOverride);
			}
		}
	}

	private void openFilesFromCommandLine(final StageManager stageManager, final CurrentProject currentProject) {
		if (runtimeOptions.getMachineFile() != null) {
			injector.getInstance(ProjectManager.class).openAutomaticProjectFromMachine(Paths.get(runtimeOptions.getMachineFile()));
		}

		if (runtimeOptions.getProject() != null) {
			injector.getInstance(ProjectManager.class).openProject(Paths.get(runtimeOptions.getProject()));
		}

		if (runtimeOptions.getMachine() != null) {
			final Machine foundMachine = currentProject.get().getMachine(runtimeOptions.getMachine());

			final Preference foundPreference;
			if (runtimeOptions.getPreference() == null) {
				foundPreference = Preference.DEFAULT;
			} else {
				foundPreference = currentProject.get().getPreference(runtimeOptions.getPreference());
			}

			if (foundMachine == null) {
				stageManager.makeAlert(Alert.AlertType.ERROR, "common.alerts.noMachine.header",
								"common.alerts.noMachine.content", runtimeOptions.getMachine(), currentProject.getName())
						.show();
			} else if (foundPreference == null) {
				stageManager.makeAlert(Alert.AlertType.ERROR, "common.alerts.noPreference.header",
								"common.alerts.noPreference.content", runtimeOptions.getPreference(), currentProject.getName())
						.show();
			} else {
				currentProject.reloadMachine(foundMachine, foundPreference);
			}
		}
	}

	private void showStartupError(final Throwable t, final Stage primaryStage) {
		primaryStage.setTitle("Error during ProB 2.0 startup");

		final String errorText = "An error occurred while starting ProB 2.0. Please copy the error information below and create a bug report at: " + BUG_REPORT_URL + "\n\n" + ExceptionAlert.getExceptionStackTrace(t);
		final TextArea errorTextArea = new TextArea(errorText);
		errorTextArea.setEditable(false);
		errorTextArea.setWrapText(true);
		// The weird height is intentional,
		// to make the text field cut off in the middle of a line.
		// This hopefully makes it more obvious to the user
		// that there is more text below that can be seen by scrolling.
		errorTextArea.setPrefHeight(390.0);
		final Scene startupErrorScene = new Scene(errorTextArea);
		primaryStage.setScene(startupErrorScene);
		primaryStage.show();
	}

	@Override
	public void start(Stage primaryStage) {
		final ImageView loadingImageView = new ImageView(ProB2.class.getResource("ProB_Logo.png").toExternalForm());
		loadingImageView.setPreserveRatio(true);
		loadingImageView.setFitHeight(100.0);
		final Scene loadingScene = new Scene(new BorderPane(loadingImageView));
		primaryStage.setScene(loadingScene);
		primaryStage.setTitle("Loading ProB 2.0...");
		primaryStage.getIcons().add(StageManager.ICON);
		primaryStage.show();

		ProB2Module module = new ProB2Module(this, runtimeOptions);
		injector = Guice.createInjector(module);

		// Ensure that MenuToolkit is loaded on the JavaFX application thread
		// (it throws an exception if loaded on any other thread).
		injector.getInstance(MenuToolkit.class);

		new Thread(() -> {
			final Scene mainScene;
			try {
				mainScene = this.startInBackground(primaryStage);
			} catch (RuntimeException | Error t) {
				logger.error("Uncaught exception during UI startup", t);
				Platform.runLater(() -> this.showStartupError(t, primaryStage));
				return;
			}
			Platform.runLater(() -> this.postStart(primaryStage, mainScene));
		}, "Main UI Loader").start();
	}

	private Scene startInBackground(final Stage primaryStage) {
		i18n = injector.getInstance(I18n.class);
		this.stopActions = injector.getInstance(StopActions.class);
		this.stopActions.add(() -> injector.getInstance(ProBInstanceProvider.class).shutdownAll());
		StageManager stageManager = injector.getInstance(StageManager.class);
		Thread.setDefaultUncaughtExceptionHandler((thread, exc) -> {
			logger.error("Uncaught exception on thread {}", thread, exc);
			Platform.runLater(() -> {
				try {
					injector.getInstance(RealTimeSimulator.class).stop();
					stageManager.makeExceptionAlert(exc, "common.alerts.internalException.header", "common.alerts.internalException.content", thread).show();
				} catch (Throwable t) {
					logger.error("An exception was thrown while handling an uncaught exception, something is really wrong!", t);
				}
			});
		});

		final Thread sharedAnimatorPreloader = new Thread(() -> injector.getInstance(MachineLoader.class).startSharedAnimator(), "Shared Animator Preloader");
		this.stopActions.add(sharedAnimatorPreloader::interrupt);
		sharedAnimatorPreloader.start();

		CurrentProject currentProject = injector.getInstance(CurrentProject.class);
		currentProject.addListener((observable, from, to) -> this.updateTitle(primaryStage));
		currentProject.currentMachineProperty().addListener((observable, from, to) -> this.updateTitle(primaryStage));
		currentProject.savedProperty().addListener((observable, from, to) -> this.updateTitle(primaryStage));

		Parent root = injector.getInstance(MainController.class);
		return new Scene(root);
	}

	private void postStart(final Stage primaryStage, final Scene mainScene) {
		primaryStage.hide();
		primaryStage.setScene(mainScene);
		primaryStage.sizeToScene();
		primaryStage.setMinWidth(1100);
		primaryStage.setMinHeight(480);
		this.updateTitle(primaryStage);

		final StageManager stageManager = injector.getInstance(StageManager.class);
		final CurrentProject currentProject = injector.getInstance(CurrentProject.class);
		stageManager.registerMainStage(primaryStage, this.getClass().getName());

		primaryStage.setOnCloseRequest(event -> handleCloseRequest(event, currentProject, stageManager));
		primaryStage.show();

		primaryStage.toFront();

		//Persistent stages are moved to front

		UIPersistence uiPersistence = injector.getInstance(UIPersistence.class);
		uiPersistence.open();

		this.openFilesFromCommandLine(stageManager, currentProject);

		ProBPluginManager pluginManager = injector.getInstance(ProBPluginManager.class);
		pluginManager.start();
	}

	private void updateTitle(final Stage stage) {
		final CurrentProject currentProject = injector.getInstance(CurrentProject.class);

		final StringBuilder title = new StringBuilder();

		final Machine currentMachine = currentProject.getCurrentMachine();
		if (currentMachine != null) {
			// we use the file name here because it is almost always the same as the machine name anyway
			// and the file name contains the extension as well which is important to differentiate machine types
			title.append(currentMachine.getLocation().getFileName());
			title.append(" - ");
		}

		final Project project = currentProject.get();
		if (project != null) {
			title.append(project.getName());
			title.append(" - ");
		}

		title.append("ProB 2.0");

		if (!currentProject.isSaved()) {
			title.append('*');
		}

		stage.setTitle(title.toString());
	}

	private RuntimeOptions parseRuntimeOptions(final String[] args) {
		logger.info("Parsing arguments: {}", (Object) args);

		final Options options = new Options();

		options.addOption(null, "help", false, "Show this help text.");
		options.addOption(null, "machine-file", true, "Open and start the specified machine file on startup. Cannot be used together with --project.");
		options.addOption(null, "project", true, "Open the specified project on startup. Cannot be used together with --machine-file.");
		options.addOption(null, "machine", true, "Start the specified machine from the project on startup. Requires --project.");
		options.addOption(null, "preference", true, "Use the specified preference set from the project when loading the machine. Requires --project and --machine.");
		options.addOption(null, "no-load-config", false, "Do not load the user config file, use the default config instead.");
		options.addOption(null, "no-save-config", false, "Do not save the user config file.");

		final CommandLineParser clParser = new DefaultParser();
		final CommandLine cl;
		try {
			cl = clParser.parse(options, args);
		} catch (ParseException e) {
			logger.error("Failed to parse command line", e);
			throw die(e.getLocalizedMessage(), 2);
		}
		logger.info("Parsed command line: args {}, options {}", cl.getArgs(), cl.getOptions());

		if (cl.hasOption("help")) {
			final HelpFormatter hf = new HelpFormatter();
			hf.printHelp("prob2-ui", options, true);
			throw die(null, 2);
		}

		if (!cl.getArgList().isEmpty()) {
			throw die("Positional arguments are not allowed: " + cl.getArgList(), 2);
		}

		if (cl.hasOption("machine-file") && cl.hasOption("project")) {
			throw die("Invalid combination of options: --machine-file and --project cannot be used together", 2);
		}

		if (cl.hasOption("machine") && !cl.hasOption("project")) {
			throw die("Invalid combination of options: --machine requires --project", 2);
		}

		if (cl.hasOption("preference") && !cl.hasOption("machine")) {
			throw die("Invalid combination of options: --preference requires --machine", 2);
		}

		final RuntimeOptions runtimeOpts = new RuntimeOptions(
				cl.getOptionValue("machine-file"),
				cl.getOptionValue("project"),
				cl.getOptionValue("machine"),
				cl.getOptionValue("preference"),
				!cl.hasOption("no-load-config"),
				!cl.hasOption("no-save-config")
		);
		logger.info("Created runtime options: {}", runtimeOpts);

		return runtimeOpts;
	}

	@Override
	public void stop() {
		// Please don't add any cleanup code for other classes here.
		// Instead, inject the StopActions singleton and use it to add the cleanup code for your class.
		// This helps with keeping all code of the class in one place.
		if (this.stopActions != null) {
			this.stopActions.run();
		}
	}

	private void handleCloseRequest(Event event, CurrentProject currentProject, StageManager stageManager) {
		if (!currentProject.isSaved()) {
			ButtonType save = new ButtonType(i18n.translate("common.buttons.save"), ButtonBar.ButtonData.YES);
			ButtonType doNotSave = new ButtonType(i18n.translate("common.buttons.doNotSave"), ButtonBar.ButtonData.NO);
			List<ButtonType> buttons = new ArrayList<>();
			buttons.add(save);
			buttons.add(ButtonType.CANCEL);
			buttons.add(doNotSave);
			Alert alert = stageManager.makeAlert(Alert.AlertType.CONFIRMATION, buttons,
					"common.alerts.unsavedProjectChanges.header", "common.alerts.unsavedProjectChanges.content",
					currentProject.getName());
			Optional<ButtonType> result = alert.showAndWait();
			if (result.isEmpty() || result.get().equals(ButtonType.CANCEL)) {
				event.consume();
			} else if (result.get().equals(save)) {
				injector.getInstance(ProjectManager.class).saveCurrentProject();
				Platform.exit();
			} else if (result.get().equals(doNotSave)) {
				Platform.exit();
			} else {
				throw new AssertionError("Unhandled button: " + result);
			}
		} else {
			injector.getInstance(Scheduler.class).stopTimer();
			Platform.exit();
		}
	}
}
