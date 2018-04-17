package de.prob2.ui.menu;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

import de.prob.animator.command.GetPreferenceCommand;
import de.prob.statespace.StateSpace;
import de.prob2.ui.internal.ProB2Module;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.preferences.GlobalPreferences;
import de.prob2.ui.project.MachineLoader;
import javafx.scene.control.Alert;

public class EditPreferencesProvider {
	private static final Logger LOGGER = LoggerFactory.getLogger(EditPreferencesProvider.class);

	private final MachineLoader machineLoader;
	private final GlobalPreferences globalPreferences;
	private final StageManager stageManager;
	private final ResourceBundle bundle;

	@Inject
	private EditPreferencesProvider(final StageManager stageManager, final MachineLoader machineLoader, final GlobalPreferences globalPreferences, final ResourceBundle bundle) {
		this.machineLoader = machineLoader;
		this.globalPreferences = globalPreferences;
		this.stageManager = stageManager;
		this.bundle = bundle;
	}

	public void showExternalEditor(Path path) {
		final StateSpace stateSpace = machineLoader.getEmptyStateSpace(globalPreferences);
		final GetPreferenceCommand cmd = new GetPreferenceCommand("EDITOR_GUI");
		stateSpace.execute(cmd);
		final File editor = new File(cmd.getValue());
		final String[] cmdline;
		if (ProB2Module.IS_MAC && editor.isDirectory()) {
			// On Mac, use the open tool to start app bundles
			cmdline = new String[] { "/usr/bin/open", "-a", editor.getAbsolutePath(), path.toString() };
		} else if (!editor.isDirectory() && System.getProperty("os.name", "").toLowerCase().contains("linux")) {
			cmdline = new String[] { "xdg-open", path.toString() };
		} else {
			// Run normal executables directly
			cmdline = new String[] { editor.getAbsolutePath(), path.toString() };
		}
		final ProcessBuilder processBuilder = new ProcessBuilder(cmdline);
		try {
			processBuilder.start();
		} catch (IOException e) {
			LOGGER.error("Failed to start external editor", e);
			stageManager.makeAlert(Alert.AlertType.ERROR, String.format(bundle.getString("project.machines.error.couldNotStartEditor"), e)).showAndWait();
		}
	}
}
