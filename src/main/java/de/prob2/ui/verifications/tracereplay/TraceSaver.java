package de.prob2.ui.verifications.tracereplay;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ResourceBundle;

import com.google.gson.Gson;

import com.google.inject.Inject;

import de.prob.check.tracereplay.PersistentTrace;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.Machine;

import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TraceSaver {
	private static final Charset TRACE_CHARSET = Charset.forName("UTF-8");
	private static final Logger LOGGER = LoggerFactory.getLogger(TraceSaver.class);

	private final Gson gson;
	private final CurrentProject currentProject;
	private final StageManager stageManager;
	private final ResourceBundle bundle;

	@Inject
	public TraceSaver(Gson gson, CurrentProject currentProject, StageManager stageManager, ResourceBundle bundle) {
		this.gson = gson;
		this.currentProject = currentProject;
		this.stageManager = stageManager;
		this.bundle = bundle;
	}

	public void saveTrace(PersistentTrace trace, Machine machine) {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(bundle.getString("verifications.tracereplay.traceSaver.dialog.title"));
		fileChooser.setInitialDirectory(currentProject.getLocation().toFile());
		fileChooser.setInitialFileName(machine.getName() + ".trace");
		fileChooser.getExtensionFilters().add(new ExtensionFilter("Trace (*.trace)", "*.trace"));
		File file = fileChooser.showSaveDialog(stageManager.getCurrent());
		
		if(file != null) {
			final Path path = file.toPath();
			try (final Writer writer = Files.newBufferedWriter(path, TRACE_CHARSET)) {
				gson.toJson(trace, writer);
			} catch (FileNotFoundException exc) {
				LOGGER.warn("Failed to create trace data file", exc);
				return;
			} catch (IOException exc) {
				LOGGER.warn("Failed to save trace", exc);
				return;
			}
			machine.addTraceFile(path);
		}
	}
}
