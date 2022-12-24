package de.prob2.ui.animation.tracereplay;

import com.fasterxml.jackson.core.JacksonException;
import com.google.inject.Inject;
import de.prob.animator.domainobjects.ErrorItem;
import de.prob.check.tracereplay.json.TraceManager;
import de.prob.check.tracereplay.json.storage.TraceJsonFile;
import de.prob.exception.ProBError;
import de.prob.json.JsonMetadata;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import de.prob2.ui.animation.symbolic.testcasegeneration.TestCaseGenerationItem;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.ProBFileHandler;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.VersionInfo;
import de.prob2.ui.internal.csv.CSVWriter;
import de.prob2.ui.operations.OperationItem;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.simulation.table.SimulationItem;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CancellationException;

public class TraceFileHandler extends ProBFileHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(TraceFileHandler.class);
	public static final String TEST_CASE_TRACE_PREFIX = "TestCaseGeneration_";
	public static final String SIMULATION_TRACE_PREFIX = "Simulation_";
	public static final String TRACE_FILE_EXTENSION = "prob2trace";
	public static final String TRACE_TABLE_EXTENSION = "csv";
	private static final int NUMBER_MAXIMUM_GENERATED_TRACES = 500;

	private final TraceManager traceManager;

	@Inject
	public TraceFileHandler(TraceManager traceManager, VersionInfo versionInfo, CurrentProject currentProject,
							StageManager stageManager, FileChooserManager fileChooserManager, I18n i18n) {
		super(versionInfo, currentProject, stageManager, fileChooserManager, i18n);
		this.traceManager = traceManager;
	}

	public void showLoadError(Path path, Throwable e) {
		if (e instanceof CancellationException) {
			// Trace check was interrupted by user, this isn't really an error.
			LOGGER.trace("Trace check interrupted", e);
			return;
		}

		LOGGER.warn("Failed to load trace file", e);
		final String headerBundleKey;
		final String contentBundleKey;
		List<Object> messageContent = new ArrayList<>();
		if (isFileNotFound(e)) {
			headerBundleKey = "animation.tracereplay.traceChecker.alerts.fileNotFound.header";
			contentBundleKey = "animation.tracereplay.traceChecker.alerts.fileNotFound.content";
			messageContent.add(path);
		} else if (isInvalidJSON(e)) {
			headerBundleKey = "animation.tracereplay.traceChecker.alerts.notAValidTraceFile.header";
			contentBundleKey = "animation.tracereplay.traceChecker.alerts.notAValidTraceFile.content";
			messageContent.add(path);
			messageContent.add(e.getMessage());
		} else {
			headerBundleKey = "animation.tracereplay.alerts.traceReplayError.header";
			contentBundleKey = "animation.tracereplay.traceChecker.alerts.traceCouldNotBeLoaded.content";
			messageContent.add(path);
		}
		stageManager.makeAlert(
				Alert.AlertType.ERROR,
				Arrays.asList(ButtonType.YES, ButtonType.NO),
				headerBundleKey,
				contentBundleKey,
				messageContent.toArray()
		).showAndWait().ifPresent(buttonType -> {
			if (buttonType.equals(ButtonType.YES)) {
				Machine currentMachine = currentProject.getCurrentMachine();
				currentMachine.getTraces().removeIf(trace -> trace.getLocation().equals(path));
			}
		});
	}

	private static boolean isFileNotFound(Throwable e) {
		if (e instanceof FileNotFoundException || e instanceof NoSuchFileException) {
			return true;
		}

		// TODO: let prolog convey this information in a cleaner way (error code, type enum, ...)
		if (e instanceof ProBError) {
			ProBError be = (ProBError) e;
			for (ErrorItem error : be.getErrors()) {
				if (error == null || error.getMessage() == null) {
					continue;
				}

				String msg = error.getMessage().toLowerCase(Locale.ROOT);
				if (msg.contains("file does not exist:")) {
					return true;
				}
			}
		}

		return e.getCause() != null && isFileNotFound(e.getCause());
	}

	private static boolean isInvalidJSON(Throwable e) {
		if (e instanceof JacksonException) {
			return true;
		}

		// TODO: let prolog convey this information in a cleaner way (error code, type enum, ...)
		if (e instanceof ProBError) {
			ProBError be = (ProBError) e;
			for (ErrorItem error : be.getErrors()) {
				if (error == null || error.getMessage() == null) {
					continue;
				}

				String msg = error.getMessage().toLowerCase(Locale.ROOT);
				if (msg.contains("parse json file:")) {
					return true;
				}
			}
		}

		return e.getCause() != null && isInvalidJSON(e.getCause());
	}

	public void addTraceFile(final Machine machine, final Path traceFilePath) {
		final Path relativeLocation = currentProject.getLocation().relativize(traceFilePath);
		machine.getTraces().add(new ReplayTrace(null, relativeLocation, traceFilePath, traceManager));
	}

	public void save(SimulationItem item, Machine machine) {
		final Path path = chooseDirectory(FileChooserManager.Kind.TRACES, "animation.tracereplay.fileChooser.savePaths.title");
		if (path == null) {
			return;
		}

		try {
			if (checkIfPathAlreadyContainsFiles(path, SIMULATION_TRACE_PREFIX, "animation.testcase.save.directoryAlreadyContainsTestCases")) {
				return;
			}

			int numberGeneratedTraces = 1; //Starts counting with 1 in the file name
			for (Trace trace : item.getTraces()) {
				final Path traceFilePath = path.resolve(SIMULATION_TRACE_PREFIX + numberGeneratedTraces + ".prob2trace");
				save(trace, traceFilePath, item.createdByForMetadata());
				this.addTraceFile(machine, traceFilePath);
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

			if (checkIfPathAlreadyContainsFiles(path, TEST_CASE_TRACE_PREFIX, "animation.testcase.save.directoryAlreadyContainsTestCases")) {
				return;
			}

			int numberGeneratedTraces = Math.min(traces.size(), NUMBER_MAXIMUM_GENERATED_TRACES);
			//Starts counting with 1 in the file name
			for (int i = 0; i < numberGeneratedTraces; i++) {
				final Path traceFilePath = path.resolve(TEST_CASE_TRACE_PREFIX + (i + 1) + ".prob2trace");
				save(traces.get(i), traceFilePath, item.createdByForMetadata(i));
				this.addTraceFile(machine, traceFilePath);
			}

		} catch (IOException e) {
			stageManager.makeExceptionAlert(e, "animation.testcase.save.error").showAndWait();
			return;
		}
		if (traces.size() > NUMBER_MAXIMUM_GENERATED_TRACES) {
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
		traceManager.save(location, new TraceJsonFile(trace, jsonMetadata));
	}

	public void save(TraceJsonFile traceFile, Path location) throws IOException {
		traceManager.save(location, traceFile);
	}

	public Path save(Trace trace, Machine machine) throws IOException {
		final Path path = openSaveFileChooser("animation.tracereplay.fileChooser.saveTrace.title", "common.fileChooser.fileTypes.proB2Trace", FileChooserManager.Kind.TRACES, TRACE_FILE_EXTENSION);
		if (path != null) {
			save(trace, path, "traceReplay");
			this.addTraceFile(machine, path);
		}
		return path;
	}

	public Path saveAsTable(Trace trace) throws IOException {
		final Path path = openSaveFileChooser("animation.tracereplay.fileChooser.saveTrace.title", "common.fileChooser.fileTypes.proB2Trace", FileChooserManager.Kind.TRACES, TRACE_TABLE_EXTENSION);
		if (path != null) {
			try (CSVWriter csvWriter = new CSVWriter(Files.newBufferedWriter(path))) {
				csvWriter.header("Position", "Transition");

				int i = 1;
				for (Transition transition : trace.getTransitionList()) {
					String name = OperationItem.forTransitionFast(trace.getStateSpace(), transition).toPrettyString(true);
					csvWriter.record(i++, name);
				}
			}
		}

		return path;
	}
}
