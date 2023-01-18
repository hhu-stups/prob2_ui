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
import de.prob2.ui.internal.ProBFileHandler;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.VersionInfo;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.simulation.configuration.SimulationConfiguration;
import de.prob2.ui.simulation.table.SimulationItem;

@Singleton
public class SimulationSaver extends ProBFileHandler {

	public static final String SIMULATION_EXTENSION = "json";
	public static final String SIMULATION_TRACE_PREFIX = "Timed_Simulation_";

	private final JacksonManager<SimulationConfiguration> jsonManager;
	private final SimulationCreator simulationCreator;

	@Inject
	public SimulationSaver(final VersionInfo versionInfo, final StageManager stageManager, final FileChooserManager fileChooserManager, final ObjectMapper objectMapper, final JacksonManager<SimulationConfiguration> jsonManager, final SimulationCreator simulationCreator,
	                       final CurrentProject currentProject, final I18n i18n) {
		super(versionInfo, currentProject, stageManager, fileChooserManager, i18n);
		this.jsonManager = jsonManager;
		this.simulationCreator = simulationCreator;

		jsonManager.initContext(new JacksonManager.Context<>(objectMapper, SimulationConfiguration.class, "Timed_Trace", SimulationConfiguration.CURRENT_FORMAT_VERSION));
	}

	public void saveConfiguration(Trace trace, List<Integer> timestamps, String createdBy) throws IOException {
		final Path path = openSaveFileChooser("simulation.tracereplay.fileChooser.saveTimedTrace.title", "common.fileChooser.fileTypes.proB2Simulation", FileChooserManager.Kind.SIMULATION, SIMULATION_EXTENSION);
		if (path != null) {
			JsonMetadata jsonMetadata = updateMetadataBuilder(SimulationConfiguration.metadataBuilder("Timed_Trace"))
				.withCreator(createdBy)
				.build();
			saveConfiguration(trace, timestamps, path, jsonMetadata);
		}
	}

	public void saveConfiguration(Trace trace, List<Integer> timestamps, Path location, JsonMetadata jsonMetadata) throws IOException {
		SimulationConfiguration configuration = simulationCreator.createConfiguration(trace, timestamps, true, jsonMetadata);
		this.jsonManager.writeToFile(location, configuration);
	}

	public void saveConfigurations(SimulationItem item) {
		List<Trace> traces = item.getTraces();
		List<List<Integer>> timestamps = item.getTimestamps();

		final Path path = chooseDirectory(FileChooserManager.Kind.SIMULATION, "simulation.tracereplay.fileChooser.saveTimedPaths.title");
		if (path == null) {
			return;
		}

		try {
			if (checkIfPathAlreadyContainsFiles(path, SIMULATION_TRACE_PREFIX, "simulation.save.directoryAlreadyContainsSimulations")) {
				return;
			}

			int numberGeneratedTraces = traces.size();
			//Starts counting with 1 in the file name
			for (int i = 1; i <= numberGeneratedTraces; i++) {
				final Path traceFilePath = path.resolve(SIMULATION_TRACE_PREFIX + i + "." + SIMULATION_EXTENSION);
				JsonMetadata jsonMetadata = updateMetadataBuilder(SimulationConfiguration.metadataBuilder("Timed_Trace"))
					.withCreator(item.createdByForMetadata())
					.build();
				this.saveConfiguration(traces.get(i - 1), timestamps.get(i - 1), traceFilePath, jsonMetadata);
			}
		} catch (IOException e) {
			stageManager.makeExceptionAlert(e, "simulation.save.error").showAndWait();
		}
	}
}
