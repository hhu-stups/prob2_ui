package de.prob2.ui.animation.tracereplay;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CancellationException;

import com.fasterxml.jackson.core.JacksonException;
import com.google.inject.Inject;

import de.prob.analysis.testcasegeneration.Target;
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
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.VersionInfo;
import de.prob2.ui.internal.csv.CSVWriter;
import de.prob2.ui.operations.OperationItem;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.simulation.table.SimulationItem;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TraceFileHandler {

	public static final String TRACE_FILE_EXTENSION = "prob2trace";

	private static final Logger LOGGER = LoggerFactory.getLogger(TraceFileHandler.class);
	private static final int NUMBER_MAXIMUM_GENERATED_TRACES = 500;

	private final TraceManager traceManager;
	private final VersionInfo versionInfo;
	private final CurrentProject currentProject;
	private final StageManager stageManager;
	private final FileChooserManager fileChooserManager;
	private final I18n i18n;

	@Inject
	public TraceFileHandler(TraceManager traceManager, VersionInfo versionInfo, CurrentProject currentProject,
	                        StageManager stageManager, FileChooserManager fileChooserManager, I18n i18n) {
		this.traceManager = traceManager;
		this.versionInfo = versionInfo;
		this.currentProject = currentProject;
		this.stageManager = stageManager;
		this.fileChooserManager = fileChooserManager;
		this.i18n = i18n;
	}

	public static boolean isFileNotFound(Throwable e) {
		if (e instanceof FileNotFoundException || e instanceof NoSuchFileException) {
			return true;
		}

		// TODO: let prolog convey this information in a cleaner way (error code, type enum, ...)
		if (e instanceof ProBError) {
			for (ErrorItem error : ((ProBError) e).getErrors()) {
				if (error != null && error.getMessage() != null) {
					if (error.getMessage().toLowerCase(Locale.ROOT).contains("file does not exist:")) {
						return true;
					}
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
			for (ErrorItem error : ((ProBError) e).getErrors()) {
				if (error != null && error.getMessage() != null) {
					String msg = error.getMessage().toLowerCase(Locale.ROOT);
					if (msg.contains("could not parse json file:") || msg.contains("json file is empty:")) {
						return true;
					}
				}
			}
		}

		return e.getCause() != null && isInvalidJSON(e.getCause());
	}

	private Alert makeTraceLoadErrorAlert(Path path, Throwable e) {
		if (e instanceof CancellationException) {
			// Trace check was interrupted by user, this isn't really an error.
			LOGGER.trace("Trace check interrupted", e);
			return null;
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
		return stageManager.makeAlert(
			Alert.AlertType.ERROR,
			headerBundleKey,
			contentBundleKey,
			messageContent.toArray()
		);
	}

	public void showLoadError(Path path, Throwable e) {
		Alert alert = makeTraceLoadErrorAlert(path, e);
		if (alert == null) {
			// No alert should be shown for this exception type (e. g. interruption).
			return;
		}
		alert.showAndWait();
	}

	public void showLoadError(ReplayTrace trace, Throwable e) {
		Alert alert = makeTraceLoadErrorAlert(trace.getAbsoluteLocation(), e);
		if (alert == null) {
			// No alert should be shown for this exception type (e. g. interruption).
			return;
		}
		alert.setContentText(alert.getContentText() + "\n\n" + i18n.translate("animation.tracereplay.traceChecker.alerts.askRemoveFromProject"));
		alert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
		alert.showAndWait().ifPresent(buttonType -> {
			if (buttonType.equals(ButtonType.YES)) {
				Machine currentMachine = currentProject.getCurrentMachine();
				currentMachine.getTraces().remove(trace);
			}
		});
	}

	public void showSaveError(Throwable e) {
		Alert alert = stageManager.makeExceptionAlert(e, "traceSave.buttons.saveTrace.error", "traceSave.buttons.saveTrace.error.msg");
		alert.showAndWait();
	}

	public ReplayTrace createReplayTraceForPath(final Path traceFilePath) {
		final Path relativeLocation = currentProject.getLocation().relativize(traceFilePath);
		return new ReplayTrace(null, relativeLocation, traceFilePath, traceManager);
	}

	public ReplayTrace addTraceFile(final Machine machine, final Path traceFilePath) {
		ReplayTrace replayTrace = createReplayTraceForPath(traceFilePath);
		Optional<ReplayTrace> existingItem = machine.getTraces().stream().filter(replayTrace::settingsEqual).findAny();
		if (existingItem.isEmpty()) {
			machine.getTraces().add(replayTrace);
			return replayTrace;
		} else {
			return existingItem.get();
		}
	}

	public void save(SimulationItem item, Machine machine) {
		DirectoryChooser fileChooser = new DirectoryChooser();
		fileChooser.setTitle(i18n.translate("animation.tracereplay.fileChooser.savePaths.title"));
		Path path = this.fileChooserManager.showDirectoryChooser(fileChooser, FileChooserManager.Kind.TRACES, stageManager.getCurrent());
		if (path == null) {
			return;
		}

		try {
			if (fileChooserManager.checkIfPathAlreadyContainsFiles(path, "Trace_", "animation.testcase.save.directoryAlreadyContainsTestCases")) {
				return;
			}

			int numberGeneratedTraces = 1; //Starts counting with 1 in the file name
			for (Trace trace : item.getTraces()) {
				final Path traceFilePath = path.resolve("Trace_" + numberGeneratedTraces + ".prob2trace");
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
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(i18n.translate("animation.tracereplay.fileChooser.saveTrace.title"));
		fileChooser.setInitialFileName(currentProject.getCurrentMachine().getName() + "TestCase." + TRACE_FILE_EXTENSION);
		fileChooser.getExtensionFilters().add(fileChooserManager.getProB2TraceFilter());
		Path path = this.fileChooserManager.showSaveFileChooser(fileChooser, FileChooserManager.Kind.TRACES, stageManager.getCurrent());

		if (path == null) {
			return;
		}

		try {

			if (fileChooserManager.checkIfPathAlreadyContainsFiles(path.getParent(), path.getFileName().toString().split("\\.")[0], "animation.testcase.save.directoryAlreadyContainsTestCases")) {
				return;
			}

			int numberGeneratedTraces = Math.min(traces.size(), NUMBER_MAXIMUM_GENERATED_TRACES);
			//Starts counting with 1 in the file name
			for (int i = 0; i < numberGeneratedTraces; i++) {
				final Path traceFilePath = path.resolve(path.toString().split("\\.")[0] + (i + 1) + ".prob2trace");
				save(traces.get(i), traceFilePath, item.createdByForMetadata(i));
				Target target = item.getResult().getTestTraces().get(i).getTarget();
				String description = "Test Case Generation Trace \nOperation: " + target.getOperation() + "\nGuard: " + target.getGuardString();
				ReplayTrace trace = this.addTraceFile(machine, traceFilePath);
				trace.saveModified(trace.load().changeDescription(description));
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
		JsonMetadata jsonMetadata = TraceJsonFile.metadataBuilder()
			                            .withProBCliVersion(versionInfo.getCliVersion().getShortVersionString())
			                            .withModelName(currentProject.getCurrentMachine().getName())
			                            .withCreator(createdBy)
			                            .build();
		traceManager.save(location, new TraceJsonFile(trace, jsonMetadata));
	}

	public Path save(Trace trace, Machine machine) throws IOException {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(i18n.translate("animation.tracereplay.fileChooser.saveTrace.title"));
		fileChooser.setInitialFileName(currentProject.getCurrentMachine().getName() + "." + TRACE_FILE_EXTENSION);
		fileChooser.getExtensionFilters().add(fileChooserManager.getProB2TraceFilter());
		Path path = this.fileChooserManager.showSaveFileChooser(fileChooser, FileChooserManager.Kind.TRACES, stageManager.getCurrent());
		if (path != null) {
			save(trace, path, "traceReplay");
			this.addTraceFile(machine, path);
		}
		return path;
	}

	public Path saveAsTable(Trace trace) throws IOException {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(i18n.translate("animation.tracereplay.fileChooser.saveTrace.title"));
		fileChooser.setInitialFileName(currentProject.getCurrentMachine().getName() + ".csv");
		fileChooser.getExtensionFilters().add(fileChooserManager.getCsvFilter());
		Path path = this.fileChooserManager.showSaveFileChooser(fileChooser, FileChooserManager.Kind.TRACES, stageManager.getCurrent());
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

	public void deleteTraceFile(ReplayTrace trace) {
		if (trace == null) {
			return;
		}

		Path path = trace.getAbsoluteLocation();
		if (path == null || !Files.isRegularFile(path)) {
			return;
		}

		Optional<ButtonType> selected = stageManager.makeAlert(
			Alert.AlertType.CONFIRMATION,
			Arrays.asList(ButtonType.YES, ButtonType.NO),
			"animation.tracereplay.dialog.deleteTraceFile.title",
			"animation.tracereplay.dialog.deleteTraceFile.content",
			path
		).showAndWait();
		if (selected.isEmpty() || selected.get() != ButtonType.YES) {
			return;
		}

		try {
			Files.delete(path);
		} catch (IOException e) {
			LOGGER.warn("could not delete trace file {}", path, e);
			stageManager.makeExceptionAlert(e, "common.alerts.couldNotDeleteFile.content").show();
		}
	}
}
