package de.prob2.ui.animation.tracereplay;

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

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.inject.Inject;

import de.prob.check.tracereplay.PersistentTrace;
import de.prob2.ui.animation.symbolic.testcasegeneration.TestCaseGenerationItem;
import de.prob2.ui.animation.symbolic.testcasegeneration.TraceInformationItem;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.json.JsonManager;
import de.prob2.ui.json.JsonMetadata;
import de.prob2.ui.json.ObjectWithMetadata;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.Machine;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TraceFileHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(TraceFileHandler.class);
	public static final String TEST_CASE_TRACE_PREFIX = "TestCaseGeneration_";
	public static final String TRACE_FILE_EXTENSION = "prob2trace";
	public static final String TRACE_FILE_PATTERN = "*." + TRACE_FILE_EXTENSION;
	private static final int NUMBER_MAXIMUM_GENERATED_TRACES = 500;

	private final JsonManager<PersistentTrace> jsonManager;
	private final CurrentProject currentProject;
	private final StageManager stageManager;
	private final FileChooserManager fileChooserManager;
	private final ResourceBundle bundle;

	@Inject
	public TraceFileHandler(JsonManager<PersistentTrace> jsonManager, CurrentProject currentProject, StageManager stageManager, FileChooserManager fileChooserManager, ResourceBundle bundle) {
		this.jsonManager = jsonManager;
		jsonManager.initContext(new JsonManager.Context<PersistentTrace>(PersistentTrace.class, "Trace", 1) {
			@Override
			public ObjectWithMetadata<JsonObject> convertOldData(final JsonObject oldObject, final JsonMetadata oldMetadata) {
				if (oldMetadata.getFileType() == null) {
					assert oldMetadata.getFormatVersion() == 0;
					if (!oldObject.has("transitionList")) {
						throw new JsonParseException("Not a valid trace file - missing required field transitionList");
					}
				}
				return new ObjectWithMetadata<>(oldObject, oldMetadata);
			}
		});
		this.currentProject = currentProject;
		this.stageManager = stageManager;
		this.fileChooserManager = fileChooserManager;
		this.bundle = bundle;
	}

	public PersistentTrace load(Path path) {
		try {
			return this.jsonManager.readFromFile(currentProject.getLocation().resolve(path)).getObject();
		} catch (FileNotFoundException | NoSuchFileException e) {
			LOGGER.warn("Trace file not found", e);
			handleFailedTraceLoad(path, e);
			return null;
		} catch (JsonParseException e) {
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
		} else if (exception instanceof JsonParseException) {
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

	public void save(TestCaseGenerationItem item, Machine machine) {
		List<PersistentTrace> traces = item.getExamples().stream()
				.map(trace -> new PersistentTrace(trace, trace.getCurrent().getIndex() + 1))
				.collect(Collectors.toList());
		@SuppressWarnings("unchecked")
		List<TraceInformationItem> traceInformation = ((List<TraceInformationItem>) item.getAdditionalInformation("traceInformation"))
				.stream()
				.filter(information -> information.getTrace() != null)
				.collect(Collectors.toList());
		final DirectoryChooser directoryChooser = new DirectoryChooser();
		directoryChooser.setTitle(bundle.getString("animation.tracereplay.fileChooser.savePaths.title"));
		final Path path = this.fileChooserManager.showDirectoryChooser(directoryChooser, FileChooserManager.Kind.TRACES, stageManager.getCurrent());
		if (path == null) {
			return;
		}

		try {
			try (final Stream<Path> children = Files.list(path)) {
				if (children.anyMatch(p -> p.getFileName().toString().startsWith(TEST_CASE_TRACE_PREFIX))) {
					// Directory already contains test case trace - ask if the user really wants to save here.
					final Optional<ButtonType> selected = stageManager.makeAlert(Alert.AlertType.WARNING, Arrays.asList(ButtonType.YES, ButtonType.NO), "", "animation.testcase.save.directoryAlreadyContainsTestCases", path).showAndWait();
					if (!selected.isPresent() || selected.get() != ButtonType.YES) {
						return;
					}
				}
			}

			int numberGeneratedTraces = Math.min(traces.size(), NUMBER_MAXIMUM_GENERATED_TRACES);
			for(int i = 0; i < numberGeneratedTraces; i++) {
				final Path traceFilePath = path.resolve(TEST_CASE_TRACE_PREFIX + i + ".prob2trace");
				String createdBy = "Test Case Generation: " + item.getName() + "; " + traceInformation.get(i);
				this.jsonManager.writeToFile(traceFilePath, traces.get(i), this.jsonManager.defaultMetadataBuilder()
					.withCreator(createdBy)
					.withCurrentModelName()
					.build());
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
	
	public void save(PersistentTrace trace, Machine machine) {
		final FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(bundle.getString("animation.tracereplay.fileChooser.saveTrace.title"));
		fileChooser.setInitialFileName(machine.getName() + "." + TRACE_FILE_EXTENSION);
		fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
			String.format(bundle.getString("common.fileChooser.fileTypes.proB2Trace"), TRACE_FILE_PATTERN),
			TRACE_FILE_PATTERN
		));
		final Path path = this.fileChooserManager.showSaveFileChooser(fileChooser, FileChooserManager.Kind.TRACES, stageManager.getCurrent());
		if (path != null) {
			save(trace, path);
			machine.addTraceFile(currentProject.getLocation().relativize(path));
		}
	}

	public void save(PersistentTrace trace, Path location) {
		try {
			this.jsonManager.writeToFile(location, trace, this.jsonManager.defaultMetadataBuilder()
				.withCreator(JsonMetadata.USER_CREATOR)
				.withCurrentModelName()
				.build());
		} catch (IOException e) {
			stageManager.makeExceptionAlert(e, "animation.tracereplay.alerts.saveError").showAndWait();
		}
	}
}
