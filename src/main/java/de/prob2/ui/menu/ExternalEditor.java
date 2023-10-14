package de.prob2.ui.menu;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.google.inject.Inject;

import de.prob2.ui.internal.ProB2Module;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.project.MachineLoader;

import javafx.scene.control.Alert;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ExternalEditor {
	private static final Logger LOGGER = LoggerFactory.getLogger(ExternalEditor.class);

	private final MachineLoader machineLoader;
	private final StageManager stageManager;

	@Inject
	private ExternalEditor(final StageManager stageManager, final MachineLoader machineLoader) {
		this.machineLoader = machineLoader;
		this.stageManager = stageManager;
	}

	private Optional<Path> getExternalEditorPath() {
		final String editorPath = machineLoader.getActiveStateSpace().getCurrentPreference("EDITOR_GUI");
		return editorPath.isEmpty() ? Optional.empty() : Optional.of(Paths.get(editorPath));
	}

	private static List<String> getCommandLine(final Path executable, final List<String> args) {
		final List<String> commandLine = new ArrayList<>();
		if (ProB2Module.IS_MAC && Files.isDirectory(executable)) {
			// On Mac, use the open tool to start app bundles
			commandLine.add("/usr/bin/open");
			commandLine.add("-a");
		}
		commandLine.add(executable.toString());
		commandLine.addAll(args);
		return commandLine;
	}

	private static List<String> getCommandLine(final Path executable, final String... args) {
		return getCommandLine(executable, Arrays.asList(args));
	}

	public void open(Path path) {
		if (path == null) {
			return;
		}

		final Optional<Path> editorPath = this.getExternalEditorPath();
		if (editorPath.isEmpty()) {
			this.stageManager.makeAlert(Alert.AlertType.ERROR, "externalEditor.alerts.noEditorSet.header", "externalEditor.alerts.noEditorSet.content").show();
			return;
		}
		final ProcessBuilder processBuilder = new ProcessBuilder(getCommandLine(editorPath.get().toAbsolutePath(), path.toString()));
		try {
			processBuilder.start();
		} catch (Exception e) {
			LOGGER.error("Failed to start external editor", e);
			stageManager.makeExceptionAlert(e, "externalEditor.alerts.couldNotStartEditor.content").showAndWait();
		}
	}
}
