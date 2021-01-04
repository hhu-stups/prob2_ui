package de.prob2.ui.animation.tracereplay;

import com.google.gson.JsonParseException;
import com.google.inject.Inject;
import de.prob.check.tracereplay.PersistentTrace;
import de.prob.check.tracereplay.TraceLoaderSaver;
import de.prob.json.JsonManager;
import de.prob.json.JsonMetadata;
import de.prob2.ui.animation.symbolic.testcasegeneration.TestCaseGenerationItem;
import de.prob2.ui.animation.symbolic.testcasegeneration.TraceInformationItem;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.VersionInfo;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.Machine;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
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
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TraceFileHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(TraceFileHandler.class);
	public static final String TEST_CASE_TRACE_PREFIX = "TestCaseGeneration_";
	public static final String TRACE_FILE_EXTENSION = "prob2trace";
	private static final int NUMBER_MAXIMUM_GENERATED_TRACES = 500;

	private final VersionInfo versionInfo;
	private final CurrentProject currentProject;
	private final StageManager stageManager;
	private final FileChooserManager fileChooserManager;
	private final ResourceBundle bundle;
	private final TraceLoaderSaver traceLoaderSaver;

	@Inject
	public TraceFileHandler(TraceLoaderSaver traceLoaderSaver, VersionInfo versionInfo, CurrentProject currentProject, StageManager stageManager, FileChooserManager fileChooserManager, ResourceBundle bundle) {
		this.traceLoaderSaver = traceLoaderSaver;
		this.versionInfo = versionInfo;
		this.currentProject = currentProject;
		this.stageManager = stageManager;
		this.fileChooserManager = fileChooserManager;
		this.bundle = bundle;
	}

	public PersistentTrace load(Path path) {
		try {
			return traceLoaderSaver.load(currentProject.getLocation().resolve(path));
		} catch (IOException | JsonParseException e) {
			this.showLoadError(path, e);
			return null;
		}
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

	public void save(TestCaseGenerationItem item, Machine machine) {
		List<PersistentTrace> traces = item.getExamples().stream()
				.map(trace -> new PersistentTrace(trace, trace.getCurrent().getIndex() + 1))
				.collect(Collectors.toList());
		List<TraceInformationItem> traceInformation = item.getTraceInformation().stream()
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
			//Starts counting with 1 in the file name
			for(int i = 1; i <= numberGeneratedTraces; i++) {
				final Path traceFilePath = path.resolve(TEST_CASE_TRACE_PREFIX + i + ".prob2trace");
				String createdBy = "Test Case Generation: " + item.getName() + "; " + traceInformation.get(i-1);
				JsonManager<PersistentTrace> jsonManager = traceLoaderSaver.getJsonManager();
				final JsonMetadata metadata = jsonManager.defaultMetadataBuilder()
					.withProBCliVersion(versionInfo.getCliVersion().getShortVersionString())
					.withModelName(machine.getName())
					.withCreator(createdBy)
					.build();
				jsonManager.writeToFile(traceFilePath, traces.get(i-1), metadata);
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
		fileChooser.getExtensionFilters().add(fileChooserManager.getExtensionFilter("common.fileChooser.fileTypes.proB2Trace", TRACE_FILE_EXTENSION));
		final Path path = this.fileChooserManager.showSaveFileChooser(fileChooser, FileChooserManager.Kind.TRACES, stageManager.getCurrent());
		if (path != null) {
			save(trace, path);
			machine.addTraceFile(currentProject.getLocation().relativize(path));
		}
	}

	public void save(PersistentTrace trace, Path location) {
		try {
			traceLoaderSaver.save(trace, location, versionInfo.getCliVersion().getShortVersionString(), currentProject.getCurrentMachine().getName());
		} catch (IOException e) {
			stageManager.makeExceptionAlert(e, "animation.tracereplay.alerts.saveError").showAndWait();
		}
	}
}
