package de.prob2.ui.project;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import com.google.inject.Inject;
import com.google.inject.Singleton;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class MachineLoader {
	private static final Logger LOGGER = LoggerFactory.getLogger(MachineLoader.class);
	private final Object openLock;
	private final Api api;
	private final CurrentProject currentProject;
	private final StageManager stageManager;
	private final AnimationSelector animations;
	private final CurrentTrace currentTrace;
	private final GlobalPreferences globalPreferences;
	private final StatusBar statusBar;

	@Inject
	public MachineLoader(final Api api, final CurrentProject currentProject, final StageManager stageManager,
			final AnimationSelector animations, final CurrentTrace currentTrace, final GlobalPreferences globalPreferences, final StatusBar statusBar) {
		this.api = api;
		this.openLock = new Object();
		this.currentProject = currentProject;
		this.stageManager = stageManager;
		this.animations = animations;
		this.currentTrace = currentTrace;
		this.globalPreferences = globalPreferences;
		this.statusBar = statusBar;
	}

	public void loadAsync(Machine machine, Map<String, String> pref) {
		new Thread(() -> {
			try {
				this.load(machine, pref);
			} catch (CliError | IOException | ModelTranslationError | ProBError e) {
				LOGGER.error("Loading machine \"{}\" failed", machine.getName(), e);
				Platform.runLater(() -> stageManager
						.makeAlert(Alert.AlertType.ERROR, "Could not open machine \"" + machine.getName() + "\":\n" + e)
						.showAndWait());
			}
		}, "Machine Loader").start();
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
				
				setLoadingStatus(StatusBar.LoadingStatus.LOADING_FILE);
				final Path path;
				if (currentProject.getMachines().contains(machine)) {
					final String projectLocation = currentProject.get().getLocation().getPath();
					path = Paths.get(projectLocation, machine.getPath().toString());
				} else {
					path = machine.getPath();
				}
				final Map<String, String> allPrefs = new HashMap<>(this.globalPreferences);
				allPrefs.putAll(prefs);
				final StateSpace stateSpace = api.b_load(path.toString(), allPrefs);
				
				setLoadingStatus(StatusBar.LoadingStatus.ADDING_ANIMATION);
				this.animations.addNewAnimation(new Trace(stateSpace));
			} finally {
				setLoadingStatus(StatusBar.LoadingStatus.NOT_LOADING);
			}
		}
	}
}
