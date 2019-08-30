package de.prob2.ui.animation.tracereplay;

import com.google.gson.Gson;
import com.google.inject.Inject;
import de.prob.check.tracereplay.PersistentTrace;
import de.prob2.ui.animation.symbolic.SymbolicAnimationFormulaItem;
import de.prob2.ui.animation.symbolic.testcasegeneration.TraceInformationItem;
import de.prob2.ui.internal.AbstractFileHandler;
import de.prob2.ui.internal.InvalidFileFormatException;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.VersionInfo;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.Machine;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.FileChooser.ExtensionFilter;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class TraceFileHandler extends AbstractFileHandler<PersistentTrace> {

	private static final int NUMBER_MAXIMUM_GENERATED_TRACES = 500;

	@Inject
	public TraceFileHandler(Gson gson, CurrentProject currentProject, StageManager stageManager, ResourceBundle bundle, VersionInfo versionInfo) {
		super(gson, currentProject, stageManager, bundle, versionInfo, PersistentTrace.class);
		this.LOGGER = LoggerFactory.getLogger(TraceFileHandler.class);
		this.FILE_ENDING = "*.prob2trace";
	}

	@Override
	public PersistentTrace load(Path path) {
		try {
			return super.load(path);
		} catch (FileNotFoundException | NoSuchFileException e) {
			LOGGER.warn("Trace file not found", e);
			handleFailedTraceLoad(path, e);
			return null;
		} catch (InvalidFileFormatException e) {
			LOGGER.warn("Invalid trace file", e);
			handleFailedTraceLoad(path, e);
			return null;
		} catch (IOException e) {
			LOGGER.warn("Failed to open trace file", e);
			handleFailedTraceLoad(path, e);
			return null;
		}
	}

	private void handleFailedTraceLoad(Path path, Exception exception) {
		Alert alert;
		List<ButtonType> buttons = new ArrayList<>();
		buttons.add(ButtonType.YES);
		buttons.add(ButtonType.NO);
		if (exception instanceof NoSuchFileException || exception instanceof FileNotFoundException) {
			alert = stageManager.makeAlert(Alert.AlertType.ERROR, buttons,
				"animation.tracereplay.traceChecker.alerts.fileNotFound.header",
				"animation.tracereplay.traceChecker.alerts.fileNotFound.content", path);
		} else if (exception instanceof InvalidFileFormatException) {
			alert = stageManager.makeAlert(Alert.AlertType.ERROR, buttons,
				"animation.tracereplay.traceChecker.alerts.notAValidTraceFile.header",
				"animation.tracereplay.traceChecker.alerts.notAValidTraceFile.content", path);
		} else {
			alert = stageManager.makeAlert(Alert.AlertType.ERROR, buttons,
				"animation.tracereplay.alerts.traceReplayError.header",
				"animation.tracereplay.traceChecker.alerts.traceCouldNotBeLoaded.content", path);
		}
		Optional<ButtonType> result = alert.showAndWait();
		if (result.isPresent() && result.get().equals(ButtonType.YES)) {
			Machine currentMachine = currentProject.getCurrentMachine();
			if (currentMachine.getTraceFiles().contains(path)) {
				currentMachine.removeTraceFile(path);
			}
		}
	}

	private void deleteFile(File file) throws IOException {
		if(file.isDirectory() && file.exists()) {
			String[] children = file.list();
			if(children != null) {
				for (String child : children) {
					deleteFile(new File(file, child));
				}
			}
		}
		Files.deleteIfExists(Paths.get(file.getAbsolutePath()));
	}

	public void save(SymbolicAnimationFormulaItem item, Machine machine) {
		List<PersistentTrace> traces = item.getExamples().stream()
				.map(trace -> new PersistentTrace(trace, trace.getCurrent().getIndex() + 1))
				.collect(Collectors.toList());
		@SuppressWarnings("unchecked")
		List<TraceInformationItem> traceInformation = ((List<TraceInformationItem>) item.getAdditionalInformation("traceInformation"))
				.stream()
				.filter(information -> information.getTrace() != null)
				.collect(Collectors.toList());
		File file = showSaveDialogForManyFiles(bundle.getString("animation.tracereplay.fileChooser.savePaths.title"), currentProject.getLocation().toFile());
		if(file == null) {
			return;
		}
		int numberGeneratedTraces = Math.min(traces.size(), NUMBER_MAXIMUM_GENERATED_TRACES);
		try {
			deleteFile(file);
			Files.createDirectory(Paths.get(file.getAbsolutePath()));
		} catch (IOException e) {
			LOGGER.warn("Failed to create directory", e);
			return;
		}
		for(int i = 0; i < numberGeneratedTraces; i++) {
			StringBuilder sb = new StringBuilder();
			sb.append("TestCaseGeneration_");
			sb.append(i);
			sb.append(".prob2trace");
			String fileName = sb.toString();
			File traceFile = new File(file.getAbsolutePath() + File.separator + fileName);
			String createdBy = "Test Case Generation: " + item.getName() + "; " + traceInformation.get(i);
			writeToFile(traceFile, traces.get(i), true, createdBy);
			final Path projectLocation = currentProject.getLocation();
			final Path absolute = traceFile.toPath();
			final Path relative = projectLocation.relativize(absolute);
			machine.addTraceFile(relative);
		}
		if(traces.size() > NUMBER_MAXIMUM_GENERATED_TRACES) {
			stageManager.makeAlert(Alert.AlertType.INFORMATION,
					"animation.symbolic.testcasegeneration.notAllTestCasesGenerated.header",
					"animation.symbolic.testcasegeneration.notAllTestCasesGenerated.content",
					NUMBER_MAXIMUM_GENERATED_TRACES).showAndWait();
		}
	}
	
	public void save(PersistentTrace trace, Machine machine) {
		File file = showSaveDialog(bundle.getString("animation.tracereplay.fileChooser.saveTrace.title"),
				currentProject.getLocation().toFile(),
				machine.getName() + FILE_ENDING.substring(1),
				new ExtensionFilter(
						String.format(bundle.getString("common.fileChooser.fileTypes.proB2Trace"), FILE_ENDING),
						FILE_ENDING));
		save(trace, file);
		if(file != null) {
			final Path projectLocation = currentProject.getLocation();
			final Path absolute = file.toPath();
			final Path relative = projectLocation.relativize(absolute);
			machine.addTraceFile(relative);
		}
	}

	public void save(PersistentTrace trace, File location) {
		writeToFile(location, trace, true, "User");
	}

	@Override
	protected boolean isValidData(PersistentTrace data) {
		return data.getTransitionList() != null;
	}
	
}
