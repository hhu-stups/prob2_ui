package de.prob2.ui.verifications.tracereplay;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.inject.Inject;

import de.prob.check.tracereplay.PersistentTrace;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.Machine;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;

public class TraceLoader {
	private static final Charset PROJECT_CHARSET = Charset.forName("UTF-8");
	private static final Logger LOGGER = LoggerFactory.getLogger(TraceLoader.class);

	private final Gson gson;
	private final StageManager stageManager;
	private final CurrentProject currentProject;

	@Inject
	public TraceLoader(Gson gson, StageManager stageManager, CurrentProject currentProject) {
		this.gson = gson;
		this.stageManager = stageManager;
		this.currentProject = currentProject;
	}

	public ReplayTrace loadTrace(Path path) {
		try (final Reader reader = Files.newBufferedReader(path, PROJECT_CHARSET)) {
			PersistentTrace pTrace = gson.fromJson(reader, PersistentTrace.class);
			return new ReplayTrace(path, pTrace);
		} catch (FileNotFoundException | NoSuchFileException exc) {
			LOGGER.warn("Trace file not found", exc);
			Alert alert = stageManager.makeAlert(AlertType.ERROR,
					"The trace file " + path + " could not be found.\n"
							+ "The file was probably moved, renamed or deleted.\n\n"
							+ "Would you like to remove this trace from the project?",
					ButtonType.YES, ButtonType.NO);
			alert.setHeaderText("Trace File not found.");
			Optional<ButtonType> result = alert.showAndWait();
			if (result.isPresent() && result.get().equals(ButtonType.YES)) {
				Platform.runLater(() -> {
					Machine currentMachine = currentProject.getCurrentMachine();
					if(currentMachine.getTraceFiles().contains(path)) {
						currentMachine.removeTraceFile(path);
					} 
				});
			}
			return null;
		} catch (IOException exc) {
			LOGGER.warn("Failed to open project file", exc);
			return null;
		}
	}
}
