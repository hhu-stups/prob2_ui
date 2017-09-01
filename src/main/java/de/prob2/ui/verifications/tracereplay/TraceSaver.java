package de.prob2.ui.verifications.tracereplay;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;

import org.hildan.fxgson.FxGson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.inject.Inject;

import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.Machine;

public class TraceSaver {
	private static final Charset TRACE_CHARSET = Charset.forName("UTF-8");
	private static final Logger LOGGER = LoggerFactory.getLogger(TraceSaver.class);

	private final Gson gson;
	private CurrentProject currentProject;
	
	@Inject
	public TraceSaver(CurrentProject currentProject) {
		this.currentProject = currentProject;
		this.gson = FxGson.coreBuilder().disableHtmlEscaping().setPrettyPrinting().create();
	}
	
	public void saveTrace(ReplayTrace trace, Machine machine) {
		File file = new File(currentProject.getLocation() + File.separator + machine.getName() + "_trace.json");
		try (final Writer writer = new OutputStreamWriter(new FileOutputStream(file), TRACE_CHARSET)) {
			gson.toJson(trace, writer);
		} catch (FileNotFoundException exc) {
			LOGGER.warn("Failed to create trace data file", exc);
		} catch (IOException exc) {
			LOGGER.warn("Failed to save trace", exc);
		}
		machine.addTrace(file);
	}

}
