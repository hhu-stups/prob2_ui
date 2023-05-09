package de.prob2.ui.simulation.simulators;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.json.JacksonManager;
import de.prob.json.JsonMetadata;
import de.prob.statespace.Trace;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.VersionInfo;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.simulation.configuration.SimulationModelConfiguration;
import de.prob2.ui.simulation.table.SimulationItem;

import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

@Singleton
public final class SimulationSaver {

	public static final String SIMULATION_EXTENSION = "json";
	public static final String SIMULATION_TRACE_PREFIX = "Timed_Simulation_";

	private final VersionInfo versionInfo;
	private final StageManager stageManager;
	private final FileChooserManager fileChooserManager;
	private final JacksonManager<SimulationModelConfiguration> jsonManager;
	private final CurrentProject currentProject;
	private final I18n i18n;

	@Inject
	public SimulationSaver(final VersionInfo versionInfo, final StageManager stageManager, final FileChooserManager fileChooserManager, final ObjectMapper objectMapper, final JacksonManager<SimulationModelConfiguration> jsonManager, final CurrentProject currentProject, final I18n i18n) {
		this.versionInfo = versionInfo;
		this.currentProject = currentProject;
		this.stageManager = stageManager;
		this.fileChooserManager = fileChooserManager;
		this.i18n = i18n;
		this.jsonManager = jsonManager;

		jsonManager.initContext(new JacksonManager.Context<>(objectMapper, SimulationModelConfiguration.class, SimulationModelConfiguration.SimulationFileType.TIMED_TRACE.getName(), SimulationModelConfiguration.CURRENT_FORMAT_VERSION));
	}

	public void saveConfiguration(Trace trace, List<Integer> timestamps, String createdBy) throws IOException {
		final FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(i18n.translate("simulation.tracereplay.fileChooser.saveTimedTrace.title"));
		fileChooser.setInitialFileName(currentProject.getCurrentMachine().getName() + "." + SIMULATION_EXTENSION);
		fileChooser.getExtensionFilters().add(fileChooserManager.getExtensionFilter("common.fileChooser.fileTypes.simulation", SIMULATION_EXTENSION));
		final Path path = this.fileChooserManager.showSaveFileChooser(fileChooser, FileChooserManager.Kind.SIMULATION, stageManager.getCurrent());
		if (path != null) {
			saveConfiguration(trace, timestamps, path, createMetadata(createdBy));
		}
	}

	private JsonMetadata createMetadata(String createdBy) {
		return SimulationModelConfiguration.metadataBuilder(SimulationModelConfiguration.SimulationFileType.TIMED_TRACE)
			.withProBCliVersion(versionInfo.getCliVersion().getShortVersionString())
			.withModelName(currentProject.getCurrentMachine().getName())
			.withCreator(createdBy)
			.build();
	}

	private void saveConfiguration(Trace trace, List<Integer> timestamps, Path location, JsonMetadata jsonMetadata) throws IOException {
		SimulationModelConfiguration configuration = SimulationCreator.createConfiguration(trace, timestamps, true, jsonMetadata);
		this.jsonManager.writeToFile(location, configuration);
	}

	public void saveConfigurations(SimulationItem item) {
		List<Trace> traces = item.getTraces();
		List<List<Integer>> timestamps = item.getTimestamps();

		final DirectoryChooser directoryChooser = new DirectoryChooser();
		directoryChooser.setTitle(i18n.translate("simulation.tracereplay.fileChooser.saveTimedPaths.title"));
		final Path path = this.fileChooserManager.showDirectoryChooser(directoryChooser, FileChooserManager.Kind.SIMULATION, stageManager.getCurrent());
		if (path == null) {
			return;
		}

		try {
			if (fileChooserManager.checkIfPathAlreadyContainsFiles(path, SIMULATION_TRACE_PREFIX, "simulation.save.directoryAlreadyContainsSimulations")) {
				return;
			}

			int numberGeneratedTraces = traces.size();
			for (int i = 1; i <= numberGeneratedTraces; i++) { //Starts counting with 1 in the file name
				final Path traceFilePath = path.resolve(SIMULATION_TRACE_PREFIX + i + "." + SIMULATION_EXTENSION);
				this.saveConfiguration(traces.get(i - 1), timestamps.get(i - 1), traceFilePath, createMetadata(item.createdByForMetadata()));
			}
		} catch (IOException e) {
			stageManager.makeExceptionAlert(e, "simulation.save.error").showAndWait();
		}
	}
}
