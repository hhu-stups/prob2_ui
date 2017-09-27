package de.prob2.ui.verifications.tracereplay;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Optional;

import org.hildan.fxgson.FxGson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.inject.Inject;

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
	public TraceLoader(StageManager stageManager, CurrentProject currentProject) {
		this.gson = FxGson.coreBuilder().disableHtmlEscaping().setPrettyPrinting().create();
		this.stageManager = stageManager;
		this.currentProject = currentProject;
	}

	public ReplayTrace loadTrace(File file) {
		ReplayTrace trace;
		try (final Reader reader = new InputStreamReader(new FileInputStream(file), PROJECT_CHARSET)) {
			trace = gson.fromJson(reader, ReplayTrace.class);
		} catch (FileNotFoundException exc) {
			LOGGER.warn("Trace file not found", exc);
			Alert alert = stageManager.makeAlert(AlertType.ERROR,
					"The trace file " + file + " could not be found.\n"
							+ "The file was probably moved, renamed or deleted.\n\n"
							+ "Would you like to remove this trace from the project?",
					ButtonType.YES, ButtonType.NO);
			alert.setHeaderText("Trace File not found.");
			Optional<ButtonType> result = alert.showAndWait();
			if (result.isPresent() && result.get().equals(ButtonType.YES)) {
				Platform.runLater(() -> {
					Machine currentMachine = currentProject.getCurrentMachine();
					if(currentMachine.getTraces().contains(file)) {
						currentMachine.removeTrace(file);
					} 
				});
			}
			return null;
		} catch (IOException exc) {
			LOGGER.warn("Failed to open project file", exc);
			return null;
		}
		return trace;
	}
}
