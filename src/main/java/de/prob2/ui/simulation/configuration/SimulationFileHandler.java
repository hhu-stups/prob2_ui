package de.prob2.ui.simulation.configuration;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.io.MoreFiles;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.json.JacksonManager;
import de.prob.json.JsonConversionException;
import de.prob.json.JsonMetadata;
import de.prob.json.JsonMetadataBuilder;
import de.prob.model.representation.Named;
import de.prob.statespace.LoadedMachine;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.VersionInfo;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.simulation.SimulationItem;
import de.prob2.ui.simulation.interactive.UIInteractionHandler;
import de.prob2.ui.simulation.simulators.RealTimeSimulator;
import de.prob2.ui.simulation.simulators.SimulationCreator;
import de.prob2.ui.simulation.simulators.Simulator;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static de.prob.statespace.Transition.INITIALISE_MACHINE_NAME;
import static de.prob.statespace.Transition.SETUP_CONSTANTS_NAME;

@Singleton
public final class SimulationFileHandler {

	public static final Path DEFAULT_SIMULATION_PATH = Path.of("");
	public static final String SIMULATION_FILE_EXTENSION = "json";
	public static final String SIMULATION_TRACE_PREFIX = "Timed_Simulation_";

	private static final Logger LOGGER = LoggerFactory.getLogger(SimulationFileHandler.class);

	private final StageManager stageManager;
	private final I18n i18n;
	private final FileChooserManager fileChooserManager;
	private final CurrentProject currentProject;
	private final UIInteractionHandler uiInteraction;
	private final RealTimeSimulator realTimeSimulator;
	private final VersionInfo versionInfo;
	private final JacksonManager<SimulationModelConfiguration> jacksonManager;
	private final ObjectMapper objectMapper;

	@Inject
	public SimulationFileHandler(StageManager stageManager, JacksonManager<SimulationModelConfiguration> jacksonManager, ObjectMapper objectMapperForJacksonManager, I18n i18n, FileChooserManager fileChooserManager, CurrentProject currentProject, UIInteractionHandler uiInteraction, RealTimeSimulator realTimeSimulator, VersionInfo versionInfo, ObjectMapper objectMapper) {
		this.stageManager = stageManager;
		this.jacksonManager = jacksonManager;
		this.i18n = i18n;
		this.fileChooserManager = fileChooserManager;
		this.currentProject = currentProject;
		this.uiInteraction = uiInteraction;
		this.realTimeSimulator = realTimeSimulator;
		this.versionInfo = versionInfo;
		this.jacksonManager.initContext(new JacksonManager.Context<>(objectMapperForJacksonManager, SimulationModelConfiguration.class, SimulationModelConfiguration.FILE_TYPE, SimulationModelConfiguration.CURRENT_FORMAT_VERSION) {
			@Override
			public boolean shouldAcceptOldMetadata() {
				// we want to support hand-written simulations without metadata
				return true;
			}

			@Override
			public ObjectNode convertOldData(ObjectNode oldObject, int oldVersion) {
				// do not throw exception when loading v0 data (without metadata)
				return oldObject;
			}

			@Override
			public boolean isFileTypeAccepted(JsonMetadata metadata) {
				// previously simulation configs were saved with these alternative file types
				return super.isFileTypeAccepted(metadata) || "Timed_Trace".equals(metadata.getFileType()) || "Interaction_Replay".equals(metadata.getFileType());
			}

			@Override
			public JsonMetadata updateMetadataOnSave(JsonMetadata metadata) {
				JsonMetadataBuilder b = new JsonMetadataBuilder(metadata)
						                        .withFormatVersion(this.currentFormatVersion)
						                        .withFileType(this.fileType)
						                        .withSavedNow()
						                        .withProB2KernelVersion(SimulationFileHandler.this.versionInfo.getKernelVersion())
						                        .withProBCliVersion(SimulationFileHandler.this.versionInfo.getCliVersion().toString());
				if (metadata.getCreator() == null) {
					b.withUserCreator();
				}
				return b.build();
			}
		});
		this.objectMapper = objectMapper;
	}

	public void initSimulator(Window window, Simulator simulator, LoadedMachine loadedMachine, Path path) {
		try {
			simulator.initSimulator(this.loadConfiguration(path, loadedMachine));
		} catch (IOException e) {
			LOGGER.error("Tried to load simulation configuration file", e);
			Platform.runLater(() -> {
				Alert alert = this.stageManager.makeExceptionAlert(e, "simulation.error.header.fileNotFound", "simulation.error.body.fileNotFound");
				alert.initOwner(window);
				alert.showAndWait();
			});
		} catch (Exception e) {
			LOGGER.error("Errors in simulation configuration file detected", e);
			Platform.runLater(() -> {
				Alert alert = this.stageManager.makeExceptionAlert(e, "simulation.error.header.configurationError", "simulation.error.body.configurationError");
				alert.initOwner(window);
				alert.showAndWait();
			});
		}
	}

	public ISimulationModelConfiguration loadConfiguration(Path path, LoadedMachine loadedMachine) throws IOException {
		if (DEFAULT_SIMULATION_PATH.equals(path)) {
			return this.createDefaultSimulation(loadedMachine);
		}

		path = path.toRealPath();
		if (Files.isDirectory(path)) {
			List<Path> timedTraces;
			try (var s = Files.walk(path)) {
				timedTraces = s
						              .filter(Files::isRegularFile)
						              .filter(p -> MoreFiles.getFileExtension(p).equals(SIMULATION_FILE_EXTENSION))
						              .sorted()
						              .collect(Collectors.toList());
			}
			return new SimulationBlackBoxModelConfiguration(timedTraces);
		} else if (SIMULATION_FILE_EXTENSION.equals(MoreFiles.getFileExtension(path))) {
			ObjectNode json;
			try (final BufferedReader reader = Files.newBufferedReader(path)) {
				json = asObject(this.objectMapper.readTree(reader));
			}
			Map<String, String> variables = buildStringMap(json.get("variables"));
			List<DiagramConfiguration> activationConfigurations = buildActivationConfigurations(json.get("activations"));
			List<UIListenerConfiguration> uiListenerConfigurations = buildUIListenerConfigurations(json.get("listeners"));
			JsonMetadata metadata = this.objectMapper.treeToValue(json.get("metadata"), JsonMetadata.class);
			if (metadata == null) {
				metadata = createMetadata(null, null);
			}

			return new SimulationModelConfiguration(variables, activationConfigurations, uiListenerConfigurations, metadata);
		} else {
			// Currently ends with py; more could be supported in the future
			return new SimulationExternalConfiguration(path);
		}
	}

	public void saveTimedTrace(Trace trace, List<Integer> timestamps, String createdBy) throws IOException {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(i18n.translate("simulation.tracereplay.fileChooser.saveTimedTrace.title"));
		fileChooser.setInitialFileName(currentProject.getCurrentMachine().getName() + "." + SimulationFileHandler.SIMULATION_FILE_EXTENSION);
		fileChooser.getExtensionFilters().add(fileChooserManager.getSimBFilter());
		Path path = this.fileChooserManager.showSaveFileChooser(fileChooser, FileChooserManager.Kind.SIMULATION, stageManager.getCurrent());
		if (path != null) {
			this.saveTimedTrace(trace, timestamps, path, createMetadata(createdBy, trace.getModel() != null && trace.getModel().getMainComponent() instanceof Named named ? named.getName() : null));
		}
	}

	public void saveUIInteractions() throws IOException {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(i18n.translate("simulation.tracereplay.fileChooser.saveUIReplay.title"));
		fileChooser.setInitialFileName(currentProject.getCurrentMachine().getName() + "." + SIMULATION_FILE_EXTENSION);
		fileChooser.getExtensionFilters().add(fileChooserManager.getExtensionFilter("common.fileChooser.fileTypes.simulation", SIMULATION_FILE_EXTENSION));
		Path path = this.fileChooserManager.showSaveFileChooser(fileChooser, FileChooserManager.Kind.SIMULATION, stageManager.getCurrent());
		if (path != null) {
			SimulationModelConfiguration configuration = uiInteraction.createUserInteractionSimulation(realTimeSimulator);
			this.saveConfiguration(configuration, path);
		}
	}

	public void saveConfiguration(SimulationModelConfiguration configuration, Path location) throws IOException {
		if (configuration != null && location != null) {
			this.jacksonManager.writeToFile(location, configuration);
		}
	}

	public void saveTimedTracesForSimulationItem(SimulationItem item) {
		SimulationItem.Result result = (SimulationItem.Result) item.getResult();
		List<Trace> traces = result.getTraces();
		List<List<Integer>> timestamps = result.getTimestamps();

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

			for (int i = 0, len = traces.size(); i < len; i++) {
				// Starts counting with 1 in the file name
				final Path traceFilePath = path.resolve(SIMULATION_TRACE_PREFIX + (i + 1) + "." + SIMULATION_FILE_EXTENSION);
				Trace trace = traces.get(i);
				this.saveTimedTrace(trace, timestamps.get(i), traceFilePath, createMetadata(item.createdByForMetadata(), trace.getModel() != null && trace.getModel().getMainComponent() instanceof Named named ? named.getName() : null));
			}
		} catch (IOException e) {
			stageManager.makeExceptionAlert(e, "simulation.save.error").showAndWait();
		}
	}

	private JsonMetadata createMetadata(String createdBy, String modelName) {
		JsonMetadataBuilder b = SimulationModelConfiguration.metadataBuilder()
				                        .withProB2KernelVersion(this.versionInfo.getKernelVersion())
				                        .withProBCliVersion(this.versionInfo.getCliVersion().toString());
		if (createdBy != null) {
			b.withCreator(createdBy);
		}
		if (modelName != null) {
			b.withModelName(modelName);
		}
		return b.build();
	}

	private void saveTimedTrace(Trace trace, List<Integer> timestamps, Path location, JsonMetadata jsonMetadata) throws IOException {
		if (location != null) {
			SimulationModelConfiguration configuration = SimulationCreator.createConfiguration(trace, timestamps, true, jsonMetadata);
			this.saveConfiguration(configuration, location);
		}
	}

	private SimulationModelConfiguration createDefaultSimulation(LoadedMachine loadedMachine) {
		Map<String, String> variables = new HashMap<>();
		List<DiagramConfiguration> activations = new ArrayList<>();
		List<UIListenerConfiguration> uiListenerConfigurations = new ArrayList<>();
		JsonMetadata metadata = createMetadata(null, null);

		if(!loadedMachine.getConstantNames().isEmpty()) {
			activations.add(new ActivationOperationConfiguration(SETUP_CONSTANTS_NAME, SETUP_CONSTANTS_NAME,
					"0", 0, null, ActivationOperationConfiguration.ActivationKind.MULTI, null, null, null, true, null, null));
		}

		Set<String> operations = loadedMachine.getOperationNames();

		activations.add(new ActivationOperationConfiguration(INITIALISE_MACHINE_NAME, INITIALISE_MACHINE_NAME,
				"0", 1, null, ActivationOperationConfiguration.ActivationKind.MULTI, null, null, List.copyOf(operations), true, null, null));

		for(String op : operations) {
			activations.add(new ActivationOperationConfiguration(op, op, "100", 0, null, ActivationOperationConfiguration.ActivationKind.SINGLE_MAX, null, "uniform", List.copyOf(operations), true, null, null));
		}

		return new SimulationModelConfiguration(variables, activations, uiListenerConfigurations, metadata);
	}

	private static List<DiagramConfiguration> buildActivationConfigurations(JsonNode json) {
		List<DiagramConfiguration> activationConfigurations = new ArrayList<>();
		for (JsonNode activationElement : asArray(json)) {
			activationConfigurations.add(buildActivationConfiguration(activationElement));
		}
		return activationConfigurations;
	}

	private static List<UIListenerConfiguration> buildUIListenerConfigurations(JsonNode json) {
		List<UIListenerConfiguration> uiListenerConfigurations = new ArrayList<>();
		if (json != null) {
			for (JsonNode uiListenerElement : asArray(json)) {
				uiListenerConfigurations.add(buildUIListenerConfiguration(uiListenerElement));
			}
		}
		return uiListenerConfigurations;
	}

	private static ActivationChoiceConfiguration buildChoiceActivationConfiguration(JsonNode json) {
		asObject(json);
		String id = asString(json.get("id"));
		Map<String, String> activations = Objects.requireNonNullElseGet(buildStringMap(json.get("chooseActivation")), HashMap::new);
		return new ActivationChoiceConfiguration(id, activations);
	}

	private static ActivationOperationConfiguration buildOperationConfiguration(JsonNode json) {
		asObject(json);
		String id = asString(json.get("id"));
		String opName = asString(json.get("execute"));
		int priority;
		if (Transition.INITIALISE_MACHINE_NAME.equals(opName)) {
			priority = 1;
		} else if (Transition.SETUP_CONSTANTS_NAME.equals(opName)) {
			priority = 0;
		} else {
			priority = json.path("priority").asInt(0);
		}
		List<String> activations = buildActivation(json.get("activating"));
		String after = asString(json.get("after"), "0");
		String additionalGuards = asString(json.get("additionalGuards"), null);
		ActivationOperationConfiguration.ActivationKind activationKind = buildActivationKind(json.get("activationKind"));
		Map<String, String> fixedVariables = buildStringMap(json.get("fixedVariables"));
		Object probabilisticVariables = buildProbability(json.get("probabilisticVariables"));
		boolean onlyWhenExecuted = json.path("activatingOnlyWhenExecuted").asBoolean(true);
		Map<String, String> updating = buildStringMap(json.get("updating"));
		String withPredicate = asString(json.get("withPredicate"), null);
		return new ActivationOperationConfiguration(id, opName, after, priority, additionalGuards, activationKind, fixedVariables, probabilisticVariables, activations, onlyWhenExecuted, updating, withPredicate);
	}

	private static DiagramConfiguration buildActivationConfiguration(JsonNode json) {
		if (json.hasNonNull("execute")) {
			return buildOperationConfiguration(json);
		} else {
			return buildChoiceActivationConfiguration(json);
		}
	}

	private static Object buildProbability(JsonNode json) {
		if (json == null || json.isNull() || json.isMissingNode()) {
			return null;
		} else if (json.isObject()) {
			Map<String, Map<String, String>> probabilities = new HashMap<>();
			for (var e : json.properties()) {
				probabilities.put(e.getKey(), Objects.requireNonNullElseGet(buildStringMap(e.getValue()), HashMap::new));
			}
			return probabilities;
		} else {
			return asString(json);
		}
	}

	private static List<String> buildActivation(JsonNode json) {
		if (json == null || json.isNull() || json.isMissingNode()) {
			return null;
		} else if (json.isArray()) {
			return buildStringList(json);
		} else {
			return new ArrayList<>(List.of(asString(json)));
		}
	}

	private static ActivationOperationConfiguration.ActivationKind buildActivationKind(JsonNode json) {
		return switch (asString(json, "multi")) {
			case "single" -> ActivationOperationConfiguration.ActivationKind.SINGLE;
			case "single:min" -> ActivationOperationConfiguration.ActivationKind.SINGLE_MIN;
			case "single:max" -> ActivationOperationConfiguration.ActivationKind.SINGLE_MAX;
			default -> ActivationOperationConfiguration.ActivationKind.MULTI;
		};
	}

	private static UIListenerConfiguration buildUIListenerConfiguration(JsonNode json) {
		asObject(json);
		String id = asString(json.get("id"));
		String event = asString(json.get("event"));
		String predicate = asString(json.get("predicate"), "1=1");
		List<String> activating = buildActivation(json.get("activating"));
		return new UIListenerConfiguration(id, event, predicate, activating);
	}

	private static ObjectNode asObject(JsonNode json) {
		if (!json.isObject()) {
			throw new JsonConversionException("value must be an object but was '" + json + "'");
		}
		return (ObjectNode) json;
	}

	private static ArrayNode asArray(JsonNode json) {
		if (!json.isArray()) {
			throw new JsonConversionException("value must be an array but was '" + json + "'");
		}
		return (ArrayNode) json;
	}

	private static String asString(JsonNode json) {
		String value = asString(json, null);
		if (value == null) {
			throw new JsonConversionException("value must be a string but was '" + json + "'");
		}
		return value;
	}

	private static String asString(JsonNode json, String defaultValue) {
		return json != null && !json.isNull() && json.isValueNode() ? json.asText(defaultValue) : defaultValue;
	}

	private static List<String> buildStringList(JsonNode json) {
		if (json == null || json.isNull() || json.isMissingNode()) {
			return null;
		} else {
			List<String> list = new ArrayList<>();
			for (JsonNode element : asArray(json)) {
				list.add(asString(element));
			}
			return list;
		}
	}

	private static Map<String, String> buildStringMap(JsonNode json) {
		if (json == null || json.isNull() || json.isMissingNode()) {
			return null;
		} else {
			Map<String, String> map = new HashMap<>();
			for (var e : asObject(json).properties()) {
				map.put(e.getKey(), asString(e.getValue()));
			}
			return map;
		}
	}
}
