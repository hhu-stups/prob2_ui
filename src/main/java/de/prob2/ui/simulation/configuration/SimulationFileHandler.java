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
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.io.MoreFiles;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.json.JsonConversionException;
import de.prob.json.JsonMetadata;
import de.prob.statespace.LoadedMachine;
import de.prob.statespace.Transition;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.simulation.simulators.Simulator;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.Window;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class SimulationFileHandler {

	public static final Path DEFAULT_SIMULATION_PATH = Path.of("");
	public static final String SIMULATION_FILE_EXTENSION = "json";

	private static final Logger LOGGER = LoggerFactory.getLogger(SimulationFileHandler.class);

	private final StageManager stageManager;
	private final ObjectMapper objectMapper;

	@Inject
	public SimulationFileHandler(StageManager stageManager, ObjectMapper objectMapper) {
		this.stageManager = stageManager;
		this.objectMapper = objectMapper;
	}

	public void initSimulator(Window window, Simulator simulator, LoadedMachine loadedMachine, Path path) {
		try {
			simulator.initSimulator(this.constructConfiguration(path, loadedMachine));
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

	public ISimulationModelConfiguration constructConfiguration(Path path, LoadedMachine loadedMachine) throws IOException {
		if (DEFAULT_SIMULATION_PATH.equals(path)) {
			return DefaultSimulationCreator.createDefaultSimulation(loadedMachine);
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
				metadata = SimulationModelConfiguration.metadataBuilder(SimulationModelConfiguration.SimulationFileType.SIMULATION).build();
			}

			return new SimulationModelConfiguration(variables, activationConfigurations, uiListenerConfigurations, metadata);
		} else {
			// Currently ends with py; more could be supported in the future
			return new SimulationExternalConfiguration(path);
		}
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
