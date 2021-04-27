package de.prob2.ui.animation.tracereplay;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

import com.google.gson.JsonParseException;
import com.google.inject.Inject;

import de.prob.check.tracereplay.PersistentTrace;
import de.prob.check.tracereplay.json.TraceManager;
import de.prob.check.tracereplay.json.storage.TraceJsonFile;
import de.prob.json.JsonMetadata;
import de.prob.statespace.Trace;
import de.prob2.ui.animation.symbolic.testcasegeneration.TestCaseGenerationItem;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.internal.ProBFileHandler;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.VersionInfo;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.simulation.table.SimulationItem;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TraceFileHandler extends ProBFileHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(TraceFileHandler.class);
	public static final String TEST_CASE_TRACE_PREFIX = "TestCaseGeneration_";
	public static final String SIMULATION_TRACE_PREFIX = "Simulation_";
	public static final String TRACE_FILE_EXTENSION = "prob2trace";
	private static final int NUMBER_MAXIMUM_GENERATED_TRACES = 500;


	private final TraceManager traceManager;


	@Inject
	public TraceFileHandler(TraceManager traceManager, VersionInfo versionInfo, CurrentProject currentProject, StageManager stageManager, FileChooserManager fileChooserManager, ResourceBundle bundle) {
		super(versionInfo, currentProject, stageManager, fileChooserManager, bundle);
		this.traceManager = traceManager;
	}


	public TraceJsonFile loadFile(Path path) {
		try {
			return traceManager.load(currentProject.getLocation().resolve(path));
		} catch (IOException e) {
			this.showLoadError(path, e);
			return null;
		}
	}

	public PersistentTrace load(Path path) {
		TraceJsonFile traceJsonFile = this.loadFile(path);
		if(traceJsonFile == null) {
			return null;
		}
		return new PersistentTrace(traceJsonFile.getDescription(), traceJsonFile.getTransitionList());
	}

	public void showLoadError(Path path, Exception e) {
		LOGGER.warn("Failed to load trace file", e);
		final String headerBundleKey;
		final String contentBundleKey;
		List<Object> messageContent = new ArrayList<>();
		if (e instanceof NoSuchFileException || e instanceof FileNotFoundException) {
			headerBundleKey = "animation.tracereplay.traceChecker.alerts.fileNotFound.header";
			contentBundleKey = "animation.tracereplay.traceChecker.alerts.fileNotFound.content";
			messageContent.add(path);
		} else if (e instanceof JsonParseException) {
			headerBundleKey = "animation.tracereplay.traceChecker.alerts.notAValidTraceFile.header";
			contentBundleKey = "animation.tracereplay.traceChecker.alerts.notAValidTraceFile.content";
			messageContent.add(path);
			messageContent.add(e.getMessage());
		} else {
			headerBundleKey = "animation.tracereplay.alerts.traceReplayError.header";
			contentBundleKey = "animation.tracereplay.traceChecker.alerts.traceCouldNotBeLoaded.content";
			messageContent.add(path);
		}
		LOGGER.info(String.valueOf(messageContent.size()));
		stageManager.makeAlert(
				Alert.AlertType.ERROR,
				Arrays.asList(ButtonType.YES, ButtonType.NO),
				headerBundleKey,
				contentBundleKey,
				messageContent.toArray()
		).showAndWait().ifPresent(buttonType -> {
			if (buttonType.equals(ButtonType.YES)) {
				Machine currentMachine = currentProject.getCurrentMachine();
				if (currentMachine.getTraceFiles().contains(path)) {
					currentMachine.removeTraceFile(path);
				}
			}
		});
	}

	public void save(SimulationItem item, Machine machine) {
		final Path path = chooseDirectory(FileChooserManager.Kind.TRACES, "animation.tracereplay.fileChooser.savePaths.title");
		if (path == null) {
			return;
		}

		try {
			if(checkIfPathAlreadyContainsFiles(path, SIMULATION_TRACE_PREFIX, "animation.testcase.save.directoryAlreadyContainsTestCases")){
				return;
			}

			int numberGeneratedTraces = 1; //Starts counting with 1 in the file name
			for(Trace trace : item.getTraces()){
				final Path traceFilePath = path.resolve(SIMULATION_TRACE_PREFIX + numberGeneratedTraces + ".prob2trace");
				save(trace, traceFilePath, item.createdByForMetadata());
				machine.addTraceFile(currentProject.getLocation().relativize(traceFilePath));
				numberGeneratedTraces++;
			}
		} catch (IOException e) {
			stageManager.makeExceptionAlert(e, "animation.testcase.save.error").showAndWait();
		}
	}

	public void save(TestCaseGenerationItem item, Machine machine) {
		List<Trace> traces = item.getExamples();

		Path path = chooseDirectory(FileChooserManager.Kind.TRACES, "animation.tracereplay.fileChooser.savePaths.title");
		if (path == null) {
			return;
		}

		try {

			if(checkIfPathAlreadyContainsFiles(path, TEST_CASE_TRACE_PREFIX, "animation.testcase.save.directoryAlreadyContainsTestCases")){
				return;
			}

			int numberGeneratedTraces = Math.min(traces.size(), NUMBER_MAXIMUM_GENERATED_TRACES);
			//Starts counting with 1 in the file name
			for(int i = 0; i < numberGeneratedTraces; i++) {
				final Path traceFilePath = path.resolve(TEST_CASE_TRACE_PREFIX + (i+1) + ".prob2trace");
				save(traces.get(i), traceFilePath, item.createdByForMetadata(i));
				machine.addTraceFile(currentProject.getLocation().relativize(traceFilePath));
			}

		} catch (IOException e) {
			stageManager.makeExceptionAlert(e, "animation.testcase.save.error").showAndWait();
			return;
		}
		if(traces.size() > NUMBER_MAXIMUM_GENERATED_TRACES) {
			stageManager.makeAlert(Alert.AlertType.INFORMATION,
					"animation.testcase.notAllTestCasesGenerated.header",
					"animation.testcase.notAllTestCasesGenerated.content",
					NUMBER_MAXIMUM_GENERATED_TRACES).showAndWait();
		}
	}


	public void save(Trace trace, Path location, String createdBy) throws IOException {
		JsonMetadata jsonMetadata = updateMetadataBuilder(TraceJsonFile.metadataBuilder())
			.withCreator(createdBy)
			.build();
		TraceJsonFile traceJsonFile = new TraceJsonFile(trace, jsonMetadata);
		traceManager.save(location, traceJsonFile);
	}

	public void save(TraceJsonFile traceFile, Path location) throws IOException {
		traceManager.save(location, traceFile);
	}


	public void save(Trace trace, Machine machine) throws IOException {
		final Path path = openSaveFileChooser("animation.tracereplay.fileChooser.saveTrace.title", "common.fileChooser.fileTypes.proB2Trace", FileChooserManager.Kind.TRACES, TRACE_FILE_EXTENSION);
		if (path != null) {
			save(trace, path, "traceReplay");
			machine.addTraceFile(currentProject.getLocation().relativize(path));
		}
	}
}
