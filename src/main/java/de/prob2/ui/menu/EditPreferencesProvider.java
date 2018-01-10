package de.prob2.ui.menu;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.inject.Inject;
import com.google.inject.Injector;

import de.prob.animator.command.GetPreferenceCommand;
import de.prob.statespace.StateSpace;

import de.prob2.ui.beditor.BEditorStage;
import de.prob2.ui.internal.ProB2Module;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.preferences.GlobalPreferences;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.MachineLoader;

import javafx.concurrent.Worker;
import javafx.scene.control.Alert;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EditPreferencesProvider {
	private static final Logger LOGGER = LoggerFactory.getLogger(EditPreferencesProvider.class);

	private final CurrentProject currentProject;
	private final Injector injector;
	private final MachineLoader machineLoader;
	private final GlobalPreferences globalPreferences;
	private final StageManager stageManager;
	private final ResourceBundle bundle;

	@Inject
	private EditPreferencesProvider(final StageManager stageManager, final CurrentProject currentProject, final Injector injector,
									final MachineLoader machineLoader, final GlobalPreferences globalPreferences, final ResourceBundle bundle) {
		this.currentProject = currentProject;
		this.injector = injector;
		this.machineLoader = machineLoader;
		this.globalPreferences = globalPreferences;
		this.stageManager = stageManager;
		this.bundle = bundle;
	}

	public void showEditorStage(Path path) {
		final BEditorStage editorStage = injector.getInstance(BEditorStage.class);
		final String text;
		try (final Stream<String> lines = Files.lines(path)) {
			text = lines.collect(Collectors.joining(System.lineSeparator()));
		} catch (IOException | UncheckedIOException e) {
			LOGGER.error("Could not read file " + path, e);
			stageManager.makeAlert(Alert.AlertType.ERROR, String.format(bundle.getString("menu.edit.errors.couldNotReadFile"), path, e)).showAndWait();
			return;
		}
		editorStage.getEngine().getLoadWorker().stateProperty().addListener((ov, oldState, newState) -> {
			if (newState == Worker.State.SUCCEEDED) {
				editorStage.setTextEditor(text, path);
			}
		});
		editorStage.setTitle(path.getFileName().toString());
		editorStage.show();
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
		} else {
			// Run normal executables directly
			cmdline = new String[] { editor.getAbsolutePath(), path.toString() };
		}
		final ProcessBuilder processBuilder = new ProcessBuilder(cmdline);
		try {
			processBuilder.start();
		} catch (IOException e) {
			LOGGER.error("Failed to start external editor", e);
			stageManager.makeAlert(Alert.AlertType.ERROR, String.format(bundle.getString("menu.edit.errors.couldNotStartEditor"), e)).showAndWait();
		}
	}
}
