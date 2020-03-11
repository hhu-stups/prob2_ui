package de.prob2.ui.animation.tracereplay;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.gson.JsonSyntaxException;
import com.google.inject.Inject;

import de.prob.check.tracereplay.PersistentTrace;
import de.prob2.ui.animation.symbolic.testcasegeneration.TestCaseGenerationItem;
import de.prob2.ui.animation.symbolic.testcasegeneration.TraceInformationItem;
import de.prob2.ui.internal.AbstractFileHandler;
import de.prob2.ui.internal.InvalidFileFormatException;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.json.JsonManager;
import de.prob2.ui.json.JsonMetadata;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.Machine;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.FileChooser.ExtensionFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TraceFileHandler extends AbstractFileHandler<PersistentTrace> {
	private static final Logger LOGGER = LoggerFactory.getLogger(TraceFileHandler.class);
	public static final String TEST_CASE_TRACE_PREFIX = "TestCaseGeneration_";
	public static final String TRACE_FILE_EXTENSION = "prob2trace";
	public static final String TRACE_FILE_PATTERN = "*." + TRACE_FILE_EXTENSION;
	private static final int NUMBER_MAXIMUM_GENERATED_TRACES = 500;

	@Inject
	public TraceFileHandler(JsonManager jsonManager, CurrentProject currentProject, StageManager stageManager, ResourceBundle bundle) {
		super(jsonManager, currentProject, stageManager, bundle, PersistentTrace.class, "Trace", 0);
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
		} catch ( JsonSyntaxException e) {
			LOGGER.warn("Invalid syntax in trace file", e);
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
		} else if (exception instanceof JsonSyntaxException) {
			alert = stageManager.makeExceptionAlert(exception,
				"animation.tracereplay.traceChecker.alerts.syntaxError.header",
				"animation.tracereplay.traceChecker.alerts.syntaxError.content", path);
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

	public void save(TestCaseGenerationItem item, Machine machine) {
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

		try (final Stream<Path> children = Files.list(file.toPath())) {
			if (children.anyMatch(p -> p.getFileName().toString().startsWith(TEST_CASE_TRACE_PREFIX))) {
				// Directory already contains test case trace - ask if the user really wants to save here.
				final Optional<ButtonType> selected = stageManager.makeAlert(Alert.AlertType.WARNING, Arrays.asList(ButtonType.YES, ButtonType.NO), "", "animation.testcase.save.directoryAlreadyContainsTestCases", file).showAndWait();
				if (!selected.isPresent() || selected.get() != ButtonType.YES) {
					return;
				}
			}
		} catch (IOException e) {
			stageManager.makeExceptionAlert(e, "animation.testcase.save.error").showAndWait();
			return;
		}

		int numberGeneratedTraces = Math.min(traces.size(), NUMBER_MAXIMUM_GENERATED_TRACES);
		for(int i = 0; i < numberGeneratedTraces; i++) {
			StringBuilder sb = new StringBuilder();
			sb.append(TEST_CASE_TRACE_PREFIX);
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
					"animation.testcase.notAllTestCasesGenerated.header",
					"animation.testcase.notAllTestCasesGenerated.content",
					NUMBER_MAXIMUM_GENERATED_TRACES).showAndWait();
		}
	}
	
	public void save(PersistentTrace trace, Machine machine) {
		File file = showSaveDialog(bundle.getString("animation.tracereplay.fileChooser.saveTrace.title"),
				currentProject.getLocation().toFile(),
				machine.getName() + "." + TRACE_FILE_EXTENSION,
				new ExtensionFilter(
					String.format(bundle.getString("common.fileChooser.fileTypes.proB2Trace"), TRACE_FILE_PATTERN),
					TRACE_FILE_PATTERN
				));
		save(trace, file);
		if(file != null) {
			final Path projectLocation = currentProject.getLocation();
			final Path absolute = file.toPath();
			final Path relative = projectLocation.relativize(absolute);
			machine.addTraceFile(relative);
		}
	}

	public void save(PersistentTrace trace, File location) {
		writeToFile(location, trace, true, JsonMetadata.USER_CREATOR);
	}

	@Override
	protected boolean isValidData(PersistentTrace data) {
		return data.getTransitionList() != null;
	}
	
}
