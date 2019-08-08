package de.prob2.ui.project;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.exception.CliError;
import de.prob.exception.ProBError;
import de.prob.scripting.ClassicalBFactory;
import de.prob.scripting.ExtractedModel;
import de.prob.scripting.ModelFactory;
import de.prob.scripting.ModelTranslationError;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.StopActions;
import de.prob2.ui.preferences.GlobalPreferences;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.statusbar.StatusBar;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.scene.control.Alert.AlertType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class MachineLoader {
	private static final Logger LOGGER = LoggerFactory.getLogger(MachineLoader.class);

	private final CurrentProject currentProject;
	private final StageManager stageManager;
	private final CurrentTrace currentTrace;
	private final GlobalPreferences globalPreferences;
	private final StatusBar statusBar;
	private final StopActions stopActions;
	private final Injector injector;

	private final ReadOnlyBooleanWrapper loading;
	private final Object openLock;
	private final Object emptyStateSpaceLock;
	private StateSpace emptyStateSpace;

	@Inject
	public MachineLoader(
		final CurrentProject currentProject,
		final StageManager stageManager,
		final CurrentTrace currentTrace,
		final GlobalPreferences globalPreferences,
		final StatusBar statusBar,
		final StopActions stopActions,
		final Injector injector
	) {
		this.currentProject = currentProject;
		this.stageManager = stageManager;
		this.currentTrace = currentTrace;
		this.globalPreferences = globalPreferences;
		this.statusBar = statusBar;
		this.stopActions = stopActions;
		this.injector = injector;

		this.loading = new ReadOnlyBooleanWrapper(this, "loading", false);
		this.openLock = new Object();
		this.emptyStateSpaceLock = new Object();
		this.emptyStateSpace = null;
	}

	public ReadOnlyBooleanProperty loadingProperty() {
		return loading.getReadOnlyProperty();
	}

	public boolean isLoading() {
		return this.loadingProperty().get();
	}

	public StateSpace getEmptyStateSpace() {
		synchronized (this.emptyStateSpaceLock) {
			if (this.emptyStateSpace == null) {
				try {
					this.emptyStateSpace = injector.getInstance(ClassicalBFactory.class)
						.create("empty", "MACHINE empty END")
						.load(this.globalPreferences);
				} catch (CliError | ProBError e) {
					throw new IllegalStateException("Failed to load empty machine, this should never happen!", e);
				}
				if (Thread.currentThread().isInterrupted()) {
					this.emptyStateSpace.kill();
				}
			}
		}
		return this.emptyStateSpace;
	}

	public void loadAsync(Machine machine, Map<String, String> pref) {
		final Thread machineLoader = new Thread(() -> {
			try {
				this.load(machine, pref);
			} catch (FileNotFoundException e) {
				LOGGER.error("Machine file of \"{}\" not found", machine.getName(), e);
				Platform.runLater(() -> stageManager
						.makeAlert(AlertType.ERROR, "project.machineLoader.alerts.fileNotFound.header",
								"project.machineLoader.alerts.fileNotFound.content", getPathToMachine(machine))
						.showAndWait());
			} catch (CliError | IOException | ModelTranslationError | ProBError e) {
				LOGGER.error("Loading machine \"{}\" failed", machine.getName(), e);
				Platform.runLater(() -> stageManager
						.makeExceptionAlert(e, "", "project.machineLoader.alerts.couldNotOpen.content", machine.getName())
						.showAndWait());
			}
		}, "Machine Loader");
		this.stopActions.add(machineLoader::interrupt);
		machineLoader.start();
	}

	private void setLoadingStatus(final StatusBar.LoadingStatus loadingStatus) {
		Platform.runLater(() -> {
			this.loading.set(loadingStatus != StatusBar.LoadingStatus.NOT_LOADING);
			this.statusBar.setLoadingStatus(loadingStatus);
		});
	}

	private void load(Machine machine, Map<String, String> prefs) throws IOException, ModelTranslationError {
		// NOTE: This method may be called from outside the JavaFX main thread,
		// for example from loadAsync.
		// This means that all JavaFX calls must be wrapped in
		// Platform.runLater.

		// Prevent multiple threads from loading a file at the same time
		synchronized (this.openLock) {
			try {
				this.currentTrace.set(null);
				setLoadingStatus(StatusBar.LoadingStatus.PARSING_FILE);
				final Path path = getPathToMachine(machine);

				final Map<String, String> allPrefs = new HashMap<>(this.globalPreferences);
				allPrefs.putAll(prefs);
				final ModelFactory<?> modelFactory = injector.getInstance(machine.getType().getModelFactoryClass());
				final ExtractedModel<?> extract = modelFactory.extract(path.toString());
				if (Thread.currentThread().isInterrupted()) {
					return;
				}
				setLoadingStatus(StatusBar.LoadingStatus.LOADING_MODEL);
				final StateSpace stateSpace = extract.load(allPrefs);
				if (Thread.currentThread().isInterrupted()) {
					stateSpace.kill();
					return;
				}
				setLoadingStatus(StatusBar.LoadingStatus.SETTING_CURRENT_MODEL);
				this.currentTrace.set(new Trace(stateSpace));
			} finally {
				setLoadingStatus(StatusBar.LoadingStatus.NOT_LOADING);
			}
		}
	}

	private Path getPathToMachine(Machine machine) {
		final Path path;
		if (currentProject.getMachines().contains(machine)) {
			path = currentProject.get().getLocation().resolve(machine.getPath());
		} else {
			path = machine.getPath();
		}
		return path;
	}
}
