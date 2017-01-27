package de.prob2.ui.project;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.scripting.Api;
import de.prob.scripting.ModelTranslationError;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import javafx.application.Platform;
import javafx.scene.control.Alert;

@Singleton
public class MachineLoader {
	private static final Logger LOGGER = LoggerFactory.getLogger(MachineLoader.class);
	private final Object openLock;
	private final Api api;
	private final CurrentProject currentProject;
	private final StageManager stageManager;
	private final AnimationSelector animations;

	@Inject
	public MachineLoader(final Api api, final CurrentProject currentProject, final StageManager stageManager,
			final AnimationSelector animations) {
		this.api = api;
		this.openLock = new Object();
		this.currentProject = currentProject;
		this.stageManager = stageManager;
		this.animations = animations;
	}

	public void loadAsync(Machine machine) {
		new Thread(() -> {
			try {
				this.load(machine);
			} catch (IOException | ModelTranslationError e) {
				LOGGER.error("Loading machine \"" + machine.getName() + "\" failed", e);
				Platform.runLater(() -> stageManager
						.makeAlert(Alert.AlertType.ERROR, "Could not open machine \"" + machine.getName() + "\":\n" + e)
						.showAndWait());
			}
		} , "File Opener Thread").start();
	}

	public void load(Machine machine) throws IOException, ModelTranslationError {
		// NOTE: This method may be called from outside the JavaFX main thread,
		// for example from openAsync.
		// This means that all JavaFX calls must be wrapped in
		// Platform.runLater.

		// Prevent multiple threads from loading a file at the same time
		synchronized (this.openLock) {
			Map<String, String> prefs = new HashMap<>();
			if (currentProject.exists()) {
				prefs = currentProject.get().getPreferences(machine);
			}
			Path path;
			if (currentProject.isSingleFile()) {
				path = machine.getPath();
			} else {
				String projectLocation = currentProject.get().getLocation().getPath();
				path = Paths.get(projectLocation, machine.getPath().toString());
			}
			final StateSpace stateSpace;
			if (prefs.isEmpty()) {
				stateSpace = api.b_load(path.toString());
			} else {
				stateSpace = api.b_load(path.toString(), prefs);
			}
			Platform.runLater(() -> this.animations.addNewAnimation(new Trace(stateSpace)));
		}
	}
}
