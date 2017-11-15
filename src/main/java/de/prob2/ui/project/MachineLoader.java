package de.prob2.ui.project;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.be4.classicalb.core.parser.node.*;

import de.prob.exception.CliError;
import de.prob.exception.ProBError;
import de.prob.scripting.Api;
import de.prob.scripting.ModelTranslationError;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.preferences.GlobalPreferences;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.statusbar.StatusBar;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class MachineLoader {
	private static final Logger LOGGER = LoggerFactory.getLogger(MachineLoader.class);
	private static final Start EMPTY_MACHINE_AST = new Start(
		new AAbstractMachineParseUnit( // pParseUnit
			new AMachineMachineVariant(), // variant
			new AMachineHeader( // header
				Collections.singletonList(new TIdentifierLiteral("empty", 1, 9)), // name
				Collections.emptyList() // parameters
			),
			Collections.emptyList() // machineClauses
		),
		new EOF(1, 18) // eof
	);

	private final Object openLock;
	private final Api api;
	private final CurrentProject currentProject;
	private final StageManager stageManager;
	private final ResourceBundle bundle;
	private final AnimationSelector animations;
	private final CurrentTrace currentTrace;
	private final GlobalPreferences globalPreferences;
	private final StatusBar statusBar;
	private final Injector injector;

	@Inject
	public MachineLoader(final Api api, final CurrentProject currentProject, final StageManager stageManager,
				final ResourceBundle bundle, final AnimationSelector animations, final CurrentTrace currentTrace,
				final GlobalPreferences globalPreferences, final StatusBar statusBar, final Injector injector) {

		this.api = api;
		this.openLock = new Object();
		this.currentProject = currentProject;
		this.stageManager = stageManager;
		this.bundle = bundle;
		this.animations = animations;
		this.currentTrace = currentTrace;
		this.globalPreferences = globalPreferences;
		this.statusBar = statusBar;
		this.injector = injector;
	}

	public StateSpace getEmptyStateSpace(final Map<String, String> prefs) {
		try {
			return api.b_load(EMPTY_MACHINE_AST, prefs);
		} catch (CliError | IOException | ModelTranslationError | ProBError e) {
			throw new IllegalStateException("Failed to load empty machine, this should never happen!", e);
		}
	}

	public void loadAsync(Machine machine, Map<String, String> pref) {
		new Thread(() -> {
			try {
				this.load(machine, pref);
			} catch (FileNotFoundException e) {
				LOGGER.error("Machine file of \"{}\" not found", machine.getName(), e);
				Platform.runLater(() -> {
					Alert alert = stageManager.makeAlert(AlertType.ERROR, String.format(bundle.getString("machineLoader.fileNotFound.content"), getPathToMachine(machine)));
					alert.setHeaderText(bundle.getString("machineLoader.fileNotFound.header"));
					alert.showAndWait();
				});
			} catch (CliError | IOException | ModelTranslationError | ProBError e) {
				LOGGER.error("Loading machine \"{}\" failed", machine.getName(), e);
				Platform.runLater(() -> stageManager.makeExceptionAlert(Alert.AlertType.ERROR,
					String.format(bundle.getString("machineLoader.couldNotOpen"), machine.getName()), e).showAndWait());
			}
		} , "Machine Loader").start();
	}

	private void setLoadingStatus(final StatusBar.LoadingStatus loadingStatus) {
		Platform.runLater(() -> this.statusBar.setLoadingStatus(loadingStatus));
	}

	private void load(Machine machine, Map<String, String> prefs) throws IOException, ModelTranslationError {
		// NOTE: This method may be called from outside the JavaFX main thread,
		// for example from loadAsync.
		// This means that all JavaFX calls must be wrapped in
		// Platform.runLater.

		// Prevent multiple threads from loading a file at the same time
		synchronized (this.openLock) {
			try {
				setLoadingStatus(StatusBar.LoadingStatus.REMOVING_OLD_ANIMATION);
				if (currentTrace.exists()) {
					this.animations.removeTrace(currentTrace.get());
				}
				injector.getInstance(ProjectView.class).disableRunconfigurationsTable(true);

				setLoadingStatus(StatusBar.LoadingStatus.LOADING_FILE);
				final Path path = getPathToMachine(machine);

				final Map<String, String> allPrefs = new HashMap<>(this.globalPreferences);
				allPrefs.putAll(prefs);
				final StateSpace stateSpace = machine.getType().getLoader().load(api, path.toString(), allPrefs);

				setLoadingStatus(StatusBar.LoadingStatus.ADDING_ANIMATION);
				this.animations.addNewAnimation(new Trace(stateSpace));
				injector.getInstance(ProjectView.class).disableRunconfigurationsTable(false);
			} finally {
				setLoadingStatus(StatusBar.LoadingStatus.NOT_LOADING);
			}
		}
	}

	private Path getPathToMachine(Machine machine) {
		final Path path;
		if (currentProject.getMachines().contains(machine)) {
			path = currentProject.get().getLocation().toPath().resolve(machine.getPath());
		} else {
			path = machine.getPath();
		}
		return path;
	}
}
