package de.prob2.ui.simulation.simulators;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob.check.tracereplay.PersistentTrace;
import de.prob.check.tracereplay.json.storage.TraceJsonFile;
import de.prob.json.JsonManager;
import de.prob.json.JsonMetadata;
import de.prob.json.JsonMetadataBuilder;
import de.prob.json.ObjectWithMetadata;
import de.prob.statespace.Trace;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.internal.ProBFileHandler;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.VersionInfo;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.simulation.configuration.SimulationConfiguration;
import de.prob2.ui.simulation.table.SimulationItem;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Stream;

@Singleton
public class SimulationSaver extends ProBFileHandler {

	public static final String SIMULATION_EXTENSION = "json";
	public static final String SIMULATION_TRACE_PREFIX = "Timed_Simulation_";

	private final JsonManager<SimulationConfiguration> jsonManager;
	private final JsonManager.Context<SimulationConfiguration> context;
	private final SimulationCreator simulationCreator;

	@Inject
	public SimulationSaver(final VersionInfo versionInfo, final StageManager stageManager, final FileChooserManager fileChooserManager, final JsonManager<SimulationConfiguration> jsonManager, final SimulationCreator simulationCreator,
						   final CurrentProject currentProject, final ResourceBundle bundle) {
		super(versionInfo, currentProject, stageManager, fileChooserManager, bundle);
		this.jsonManager = jsonManager;
		this.simulationCreator = simulationCreator;

		final Gson gson = new GsonBuilder()
				.disableHtmlEscaping()
				.serializeNulls()
				.setPrettyPrinting()
				.create();
		this.context = new JsonManager.Context<SimulationConfiguration>(gson, SimulationConfiguration.class, "Timed_Trace", 1) {
			@Override
			public ObjectWithMetadata<JsonObject> convertOldData(final JsonObject oldObject, final JsonMetadata oldMetadata) {
				return new ObjectWithMetadata<>(oldObject, oldMetadata);
			}
		};
		jsonManager.initContext(context);
	}

	public void saveConfiguration(Trace trace, List<Integer> timestamps, String createdBy) throws IOException {
		final Path path = openSaveFileChooser("simulation.tracereplay.fileChooser.saveTimedTrace.title", "common.fileChooser.fileTypes.proB2Simulation", FileChooserManager.Kind.SIMULATION, SIMULATION_EXTENSION);
		if (path != null) {
			JsonMetadata jsonMetadata = createMetadata(createdBy);
			saveConfiguration(trace, timestamps, path, jsonMetadata);
		}
	}


	public void saveConfiguration(Trace trace, List<Integer> timestamps, Path location, JsonMetadata jsonMetadata) throws IOException {
		SimulationConfiguration configuration = simulationCreator.createConfiguration(trace, timestamps, true);
		this.jsonManager.writeToFile(location, configuration, jsonMetadata);
	}

	public void saveConfigurations(SimulationItem item) {
		List<Trace> traces = item.getTraces();
		List<List<Integer>> timestamps = item.getTimestamps();

		final Path path = chooseDirectory(FileChooserManager.Kind.SIMULATION, "simulation.tracereplay.fileChooser.saveTimedPaths.title");
		if (path == null) {
			return;
		}

		try {
			if(checkIfPathAlreadyContainsFiles(path, SIMULATION_TRACE_PREFIX, "simulation.save.directoryAlreadyContainsSimulations")){
				return;
			}

			int numberGeneratedTraces = traces.size();
			//Starts counting with 1 in the file name
			for(int i = 1; i <= numberGeneratedTraces; i++) {
				final Path traceFilePath = path.resolve(SIMULATION_TRACE_PREFIX + i + "." + SIMULATION_EXTENSION);
				JsonMetadata jsonMetadata = createMetadata(item.createdByForMetadata());
				this.saveConfiguration(traces.get(i-1), timestamps.get(i-1), traceFilePath, jsonMetadata);
			}
		} catch (IOException e) {
			stageManager.makeExceptionAlert(e, "simulation.save.error").showAndWait();
		}
	}

	@Override
	protected JsonMetadataBuilder metadataBuilder() {
		return context.getDefaultMetadataBuilder();
	}

}
