package de.prob2.ui.menu;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.inject.Inject;

import de.prob.animator.command.GetPreferenceCommand;
import de.prob.statespace.StateSpace;
import de.prob2.ui.internal.ProB2Module;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.project.MachineLoader;

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

	private Path getExternalEditorPath() {
		final StateSpace stateSpace = machineLoader.getEmptyStateSpace();
		final GetPreferenceCommand cmd = new GetPreferenceCommand("EDITOR_GUI");
		stateSpace.execute(cmd);
		return Paths.get(cmd.getValue());
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
		final ProcessBuilder processBuilder = new ProcessBuilder(getCommandLine(this.getExternalEditorPath().toAbsolutePath(), path.toString()));
		try {
			processBuilder.start();
		} catch (IOException e) {
			LOGGER.error("Failed to start external editor", e);
			stageManager.makeExceptionAlert(e, "externalEditor.alerts.couldNotStartEditor.content").showAndWait();
		}
	}
}
