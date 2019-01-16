package de.prob2.ui.animation.tracereplay;

import java.io.File;
import java.nio.file.Path;
import java.util.ResourceBundle;

import com.google.gson.Gson;
import com.google.inject.Inject;

import de.prob.check.tracereplay.PersistentTrace;
import de.prob2.ui.internal.AbstractFileHandler;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.VersionInfo;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.Machine;

import javafx.stage.FileChooser.ExtensionFilter;

import org.slf4j.LoggerFactory;

public class TraceFileHandler extends AbstractFileHandler<PersistentTrace> {

	@Inject
	public TraceFileHandler(Gson gson, CurrentProject currentProject, StageManager stageManager, ResourceBundle bundle, VersionInfo versionInfo) {
		super(gson, currentProject, stageManager, bundle, versionInfo, PersistentTrace.class);
		this.LOGGER = LoggerFactory.getLogger(TraceFileHandler.class);
		this.FILE_ENDING = "*.prob2trace";
	}
	
	public void save(PersistentTrace trace, Machine machine) {
		File file = showSaveDialog(bundle.getString("animation.tracereplay.fileChooser.saveTrace.title"),
				currentProject.getLocation().toFile(), 
				machine.getName() + FILE_ENDING.substring(1),
				new ExtensionFilter(
						String.format(bundle.getString("common.fileChooser.fileTypes.proB2Trace"), FILE_ENDING),
						FILE_ENDING));
		writeToFile(file, trace, true);
		if(file != null) {
			final Path projectLocation = currentProject.getLocation();
			final Path absolute = file.toPath();
			final Path relative = projectLocation.relativize(absolute);
			machine.addTraceFile(relative);
		}
	}

	@Override
	protected boolean isValidData(PersistentTrace data) {
		return data.getTransitionList() != null;
	}
	
}
