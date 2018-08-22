package de.prob2.ui.animation.tracereplay;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import com.google.inject.Inject;

import de.prob.check.tracereplay.PersistentTrace;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.VersionInfo;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.Machine;

import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TraceSaver {
	private static final Charset TRACE_CHARSET = Charset.forName("UTF-8");
	private static final Logger LOGGER = LoggerFactory.getLogger(TraceSaver.class);
	private static final String TRACE_FILE_ENDING = "*.prob2trace";

	private final Gson gson;
	private final CurrentProject currentProject;
	private final StageManager stageManager;
	private final ResourceBundle bundle;
	private final VersionInfo versionInfo;

	@Inject
	public TraceSaver(Gson gson, CurrentProject currentProject, StageManager stageManager, ResourceBundle bundle, VersionInfo versionInfo) {
		this.gson = gson;
		this.currentProject = currentProject;
		this.stageManager = stageManager;
		this.bundle = bundle;
		this.versionInfo = versionInfo;
	}

	public void saveTrace(PersistentTrace trace, Machine machine) {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(bundle.getString("animation.tracereplay.fileChooser.saveTrace.title"));
		fileChooser.setInitialDirectory(currentProject.getLocation().toFile());
		fileChooser.setInitialFileName(machine.getName() + TRACE_FILE_ENDING);
		fileChooser.getExtensionFilters()
				.add(new ExtensionFilter(
						String.format(bundle.getString("common.fileChooser.fileTypes.proB2Trace"), TRACE_FILE_ENDING),
						TRACE_FILE_ENDING));
		File file = fileChooser.showSaveDialog(stageManager.getCurrent());
		
		if(file != null) {
			final Path projectLocation = currentProject.getLocation();
			final Path absolute = file.toPath();
			final Path relative = projectLocation.relativize(absolute);
			
			try (final Writer writer = Files.newBufferedWriter(absolute, TRACE_CHARSET)) {
				gson.toJson(trace, writer);
				
				JsonObject metadata = new JsonObject();
				metadata.addProperty("Creation Date", ZonedDateTime.now().format(DateTimeFormatter.ofPattern("d MMM yyyy hh:mm:ssa O")));
				metadata.addProperty("ProB 2.0 kernel Version", versionInfo.getKernelVersion());
				metadata.addProperty("ProB CLI Version", versionInfo.getFormattedCliVersion());
				metadata.addProperty("Model", currentProject.getCurrentMachine().getName());
				gson.toJson(metadata, writer);
			} catch (FileNotFoundException exc) {
				LOGGER.warn("Failed to create trace data file", exc);
				return;
			} catch (IOException exc) {
				LOGGER.warn("Failed to save trace", exc);
				return;
			}
			machine.addTraceFile(relative);
		}
	}
}
