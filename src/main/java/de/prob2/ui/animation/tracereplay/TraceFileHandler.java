package de.prob2.ui.animation.tracereplay;

import com.fasterxml.jackson.databind.exc.ValueInstantiationException;
import com.google.gson.JsonParseException;
import com.google.inject.Inject;
import de.prob.check.tracereplay.PersistentTrace;
import de.prob.check.tracereplay.TraceLoaderSaver;
import de.prob.check.tracereplay.json.TraceManager;
import de.prob.check.tracereplay.json.storage.TraceJsonFile;
import de.prob.json.JsonManager;
import de.prob.json.JsonMetadata;
import de.prob.json.JsonMetadataBuilder;
import de.prob.statespace.LoadedMachine;
import de.prob.statespace.Trace;
import de.prob2.ui.animation.symbolic.testcasegeneration.TestCaseGenerationItem;
import de.prob2.ui.animation.symbolic.testcasegeneration.TraceInformationItem;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.simulation.table.SimulationItem;
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
import de.prob2.ui.internal.VersionInfo;


import static java.util.stream.Collectors.toList;

public class TraceFileHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(TraceFileHandler.class);
	public static final String TEST_CASE_TRACE_PREFIX = "TestCaseGeneration_";
	public static final String SIMULATION_TRACE_PREFIX = "Simulation_";
	public static final String TRACE_FILE_EXTENSION = "prob2trace";
	private static final int NUMBER_MAXIMUM_GENERATED_TRACES = 500;

	private final CurrentProject currentProject;
	private final StageManager stageManager;
	private final FileChooserManager fileChooserManager;
	private final ResourceBundle bundle;
	private final TraceManager traceManager;
	private final VersionInfo versionInfo;
	private final TraceLoaderSaver traceLoaderSaver;

	@Inject
	public TraceFileHandler(TraceManager traceManger, TraceLoaderSaver traceLoaderSaver, VersionInfo versionInfo, CurrentProject currentProject, StageManager stageManager, FileChooserManager fileChooserManager, ResourceBundle bundle) {
		this.versionInfo = versionInfo;
		this.traceLoaderSaver = traceLoaderSaver;
		this.currentProject = currentProject;
		this.stageManager = stageManager;
		this.fileChooserManager = fileChooserManager;
		this.bundle = bundle;
		this.traceManager = traceManger;
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
		LOGGER.debug(path.toString());
		try {
			try{
				TraceJsonFile traceJsonFile = traceManager.load(currentProject.getLocation().resolve(path));
				return new PersistentTrace(traceJsonFile.getDescription(), traceJsonFile.getTransitionList());
			}catch (ValueInstantiationException e){
				return traceLoaderSaver.load(currentProject.getLocation().resolve(path));
			}
		} catch (IOException e) {
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

	public void save(SimulationItem item, Machine machine) {

		final Path path = chooseDirectory();
		if (path == null) {
			return;
		}

		try {

			if(checkIfPathAlreadyContainsFiles(path, SIMULATION_TRACE_PREFIX)){
				return;
			}

			int numberGeneratedTraces = 1; //Starts counting with 1 in the file name
			for(Trace trace : item.getTraces()){
				final Path traceFilePath = path.resolve(SIMULATION_TRACE_PREFIX + numberGeneratedTraces + ".prob2trace");
				String createdBy = "Simulation: " + item.getTypeAsName() + "; " + item.getConfiguration();
				save(trace, traceFilePath, machine.getName(), createdBy);
				machine.addTraceFile(currentProject.getLocation().relativize(traceFilePath));
				numberGeneratedTraces++;
			}
		} catch (IOException e) {
			stageManager.makeExceptionAlert(e, "animation.testcase.save.error").showAndWait();
		}
	}

	public void save(TestCaseGenerationItem item, Machine machine) {
		List<Trace> traces = new ArrayList<>(item.getExamples());
		List<TraceInformationItem> traceInformation = item.getTraceInformation().stream()
				.filter(information -> information.getTrace() != null)
				.collect(toList());

		Path path = chooseDirectory();
		if (path == null) {
			return;
		}

		try {

			if(checkIfPathAlreadyContainsFiles(path, TEST_CASE_TRACE_PREFIX)){
				return;
			}

			int numberGeneratedTraces = Math.min(traces.size(), NUMBER_MAXIMUM_GENERATED_TRACES);
			//Starts counting with 1 in the file name
			for(int i = 0; i < numberGeneratedTraces; i++) {
				final Path traceFilePath = path.resolve(TEST_CASE_TRACE_PREFIX + (i+1) + ".prob2trace");
				String createdBy = "Test Case Generation: " + item.getName() + "; " + traceInformation.get(i);
				save(traces.get(i), traceFilePath, machine.getName(), createdBy);
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

	public Path chooseDirectory(){
		final DirectoryChooser directoryChooser = new DirectoryChooser();
		directoryChooser.setTitle(bundle.getString("animation.tracereplay.fileChooser.savePaths.title"));
		return this.fileChooserManager.showDirectoryChooser(directoryChooser, FileChooserManager.Kind.TRACES, stageManager.getCurrent());
	}

	public boolean checkIfPathAlreadyContainsFiles(Path path, String prefix) throws IOException {
		try (final Stream<Path> children = Files.list(path)) {
			if (children.anyMatch(p -> p.getFileName().toString().startsWith(prefix))) {
				// Directory already contains test case trace - ask if the user really wants to save here.
				final Optional<ButtonType> selected = stageManager.makeAlert(Alert.AlertType.WARNING, Arrays.asList(ButtonType.YES, ButtonType.NO), "", "animation.testcase.save.directoryAlreadyContainsTestCases", path).showAndWait();
				if (!selected.isPresent() || selected.get() != ButtonType.YES) {
					return true;
				}
			}
		}
		return false;
	}

	public void save(Trace trace, Path location, String machineName, String createdBy) throws IOException {
		JsonMetadata jsonMetadata = new JsonMetadataBuilder("Trace", 2)
				.withProBCliVersion(versionInfo.getCliVersion().getShortVersionString())
				.withModelName(machineName)
				.withCreator(createdBy)
				.build();
		TraceJsonFile traceJsonFile = new TraceJsonFile(trace, jsonMetadata);
		traceManager.save(location, traceJsonFile);
	}

	public void save(TraceJsonFile traceFile, Path location) throws IOException {
		traceManager.save(location, traceFile);
	}


	public void save(Trace trace, Machine machine) throws IOException {
		final FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(bundle.getString("animation.tracereplay.fileChooser.saveTrace.title"));
		fileChooser.setInitialFileName(machine.getName() + "." + TRACE_FILE_EXTENSION);
		fileChooser.getExtensionFilters().add(fileChooserManager.getExtensionFilter("common.fileChooser.fileTypes.proB2Trace", TRACE_FILE_EXTENSION));
		final Path path = this.fileChooserManager.showSaveFileChooser(fileChooser, FileChooserManager.Kind.TRACES, stageManager.getCurrent());
		if (path != null) {
			save(trace, path, machine.getName(), "traceReplay");
			machine.addTraceFile(currentProject.getLocation().relativize(path));
		}
	}

}
