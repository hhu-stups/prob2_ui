package de.prob2.ui.verifications.tracereplay;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;

import de.prob.check.tracereplay.PersistentTrace;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.Machine;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

public class TraceSaver {
	private static final Charset TRACE_CHARSET = Charset.forName("UTF-8");
	private static final Logger LOGGER = LoggerFactory.getLogger(TraceSaver.class);

	private final Gson gson;
	private final CurrentProject currentProject;
	private final StageManager stageManager;
	private final ResourceBundle bundle;

	@Inject
	public TraceSaver(CurrentProject currentProject, StageManager stageManager, ResourceBundle bundle) {
		this.currentProject = currentProject;
		this.stageManager = stageManager;
		this.bundle = bundle;
		this.gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
	}

	public void saveTrace(PersistentTrace trace, Machine machine) {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(bundle.getString("verifications.tracereplay.traceSaver.dialog.title"));
		fileChooser.setInitialDirectory(currentProject.getLocation());
		fileChooser.setInitialFileName(machine.getName());
		fileChooser.getExtensionFilters().add(new ExtensionFilter("Trace (*.trace)", "*.trace"));
		File file = fileChooser.showSaveDialog(stageManager.getCurrent());

		try (final Writer writer = new OutputStreamWriter(new FileOutputStream(file), TRACE_CHARSET)) {
			gson.toJson(trace, writer);
		} catch (FileNotFoundException exc) {
			LOGGER.warn("Failed to create trace data file", exc);
			return;
		} catch (IOException exc) {
			LOGGER.warn("Failed to save trace", exc);
			return;
		}
		machine.addTraceFile(file);
	}
}
