package de.prob2.ui.project;

import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.scripting.Api;
import de.prob.scripting.ModelTranslationError;
import de.prob.statespace.StateSpace;
import de.prob2.ui.prob2fx.CurrentProject;
import javafx.application.Platform;
import javafx.scene.control.Alert;

@Singleton
public class MachineLoader {
	private static final Logger logger = LoggerFactory.getLogger(MachineLoader.class);
	private Object openLock;
	private Api api;
	private CurrentProject currentProject;
	private StateSpace stateSpace;

	@Inject
	public MachineLoader(final Api api, final CurrentProject currentProject) {
		this.api = api;
		this.openLock = new Object();
		this.currentProject = currentProject;
	}

	public void loadAsync(Machine machine) {
		new Thread(() -> this.load(machine), "File Opener Thread").start();
		return;
	}

	public StateSpace load(Machine machine) {
		// NOTE: This method may be called from outside the JavaFX main thread,
		// for example from openAsync.
		// This means that all JavaFX calls must be wrapped in
		// Platform.runLater.

		// Prevent multiple threads from loading a file at the same time
		synchronized (this.openLock) {
			Map<String, String> prefs = currentProject.get().getPreferences(machine);
			try {
				if (!prefs.isEmpty()) {
					stateSpace = api.b_load(machine.getPath(), prefs);
				} else {
					stateSpace = api.b_load(machine.getPath());
				}
			} catch (IOException | ModelTranslationError e) {
				logger.error("loading file failed", e);
				Platform.runLater(() -> {
					Alert alert = new Alert(Alert.AlertType.ERROR, "Could not open file:\n" + e);
					alert.getDialogPane().getStylesheets().add("prob.css");
					alert.show();
				});
				return null;
			}
			return stateSpace;
		}
	}
}
