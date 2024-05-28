package de.prob2.ui.simulation.configuration;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fatboyindustrial.gsonjavatime.Converters;
import com.google.common.io.MoreFiles;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import de.prob.json.JsonMetadata;
import de.prob.statespace.LoadedMachine;
import de.prob.statespace.Transition;

public class SimulationFileHandler {

	public static final String TRACE_FILE_EXTENSION = "json";

	public static final String SIMULATION_FILE_EXTENSION = "json";

	private static final Gson METADATA_GSON = Converters.registerAll(new GsonBuilder())
			.disableHtmlEscaping()
			.serializeNulls()
			.setPrettyPrinting()
			.create();

	public static ISimulationModelConfiguration constructConfiguration(Path inputFile, LoadedMachine loadedMachine) throws IOException, JsonSyntaxException {
		if(inputFile.equals(Paths.get(""))) {
			return DefaultSimulationCreator.createDefaultSimulation(loadedMachine);
		}
		if(!inputFile.toFile().isDirectory()) {
			if(inputFile.toFile().getName().endsWith("json")) {
				Gson gson = new Gson();
				final JsonObject simulationFile;
				try (final BufferedReader reader = Files.newBufferedReader(inputFile)) {
					simulationFile = gson.fromJson(reader, JsonObject.class);
				}
				Map<String, String> variables = buildVariables(simulationFile.get("variables"));
				List<DiagramConfiguration> activationConfigurations = buildActivationConfigurations(simulationFile.get("activations"));
				List<UIListenerConfiguration> uiListenerConfigurations = simulationFile.get("listeners") == null ? new ArrayList<>() : buildUIListenerConfigurations(simulationFile.get("listeners"));
				final JsonMetadata metadata = METADATA_GSON.fromJson(simulationFile.get("metadata"), JsonMetadata.class);
				return new SimulationModelConfiguration(variables, activationConfigurations, uiListenerConfigurations, metadata);
			} else { // Currently ends with py; more could be supported in the future
				return new SimulationExternalConfiguration(inputFile);
			}
		}

		List<Path> timedTraces;
		try (var s = Files.walk(inputFile)) {
			timedTraces = s
				              .filter(Files::isRegularFile)
				              .filter(p -> MoreFiles.getFileExtension(p).equals(SimulationFileHandler.TRACE_FILE_EXTENSION))
				              .sorted()
				              .collect(Collectors.toList());
		}
		return new SimulationBlackBoxModelConfiguration(timedTraces);
	}

	private static List<DiagramConfiguration> buildActivationConfigurations(JsonElement jsonElement) {
		List<DiagramConfiguration> activationConfigurations = new ArrayList<>();
		JsonArray activationConfigurationsAsArray = jsonElement.getAsJsonArray();
		for (JsonElement activationElement : activationConfigurationsAsArray) {
			DiagramConfiguration activationConfiguration = buildActivationConfiguration(activationElement);
			activationConfigurations.add(activationConfiguration);
		}
		return activationConfigurations;
	}

	private static List<UIListenerConfiguration> buildUIListenerConfigurations(JsonElement jsonElement) {
		List<UIListenerConfiguration> uiListenerConfigurations = new ArrayList<>();
		JsonArray uiListenerConfigurationsAsArray = jsonElement.getAsJsonArray();
		for (JsonElement uiListenerElement : uiListenerConfigurationsAsArray) {
			UIListenerConfiguration uiListenerConfiguration = buildUIListenerConfiguration(uiListenerElement);
			uiListenerConfigurations.add(uiListenerConfiguration);
		}
		return uiListenerConfigurations;
	}

	private static ActivationChoiceConfiguration buildChoiceActivationConfiguration(JsonElement activationElement) {
		JsonObject activationAsObject = activationElement.getAsJsonObject();
		String id = activationAsObject.get("id").getAsString();
		JsonObject chooseActivationAsObject = activationAsObject.getAsJsonObject("chooseActivation");
		Map<String, String> activations = new HashMap<>();
		for(String key : chooseActivationAsObject.keySet()) {
			activations.put(key, chooseActivationAsObject.get(key).getAsString());
		}
		return new ActivationChoiceConfiguration(id, activations);
	}

	private static ActivationOperationConfiguration buildOperationConfiguration(JsonElement activationElement) {
		JsonObject activationAsObject = activationElement.getAsJsonObject();
		String id = activationAsObject.get("id").getAsString();
		String opName = activationAsObject.get("execute").getAsString();

		int priority;
		if(Transition.INITIALISE_MACHINE_NAME.equals(opName)) {
			priority = 1;
		} else if(Transition.SETUP_CONSTANTS_NAME.equals(opName) || activationAsObject.get("priority") == null) {
			priority = 0;
		} else {
			priority = activationAsObject.get("priority").getAsInt();
		}
		List<String> activations = buildActivation(activationAsObject.get("activating"));

		String after = activationAsObject.get("after") == null || activationAsObject.get("after").isJsonNull() ? "0" : activationAsObject.get("after").getAsString();
		String additionalGuards = activationAsObject.get("additionalGuards") == null || activationAsObject.get("additionalGuards").isJsonNull() ? null : activationAsObject.get("additionalGuards").getAsString();
		ActivationOperationConfiguration.ActivationKind activationKind = buildActivationKind(activationAsObject.get("activationKind"));
		Map<String, String> fixedVariables = buildParameters(activationAsObject.get("fixedVariables"));
		Object probabilisticVariables = buildProbability(activationAsObject.get("probabilisticVariables"));
		boolean onlyWhenExecuted = activationAsObject.get("activatingOnlyWhenExecuted") == null || activationAsObject.get("activatingOnlyWhenExecuted").getAsBoolean();
		Map<String, String> updating = buildUpdating(activationAsObject.get("updating"));
		String withPredicate = activationAsObject.get("withPredicate") == null || activationAsObject.get("withPredicate").isJsonNull() ? null : activationAsObject.get("withPredicate").getAsString();
		return new ActivationOperationConfiguration(id, opName, after, priority, additionalGuards, activationKind, fixedVariables, probabilisticVariables, activations, onlyWhenExecuted, updating, withPredicate);
	}

	private static Map<String, String> buildVariables(JsonElement jsonElement) {
		Map<String, String> updating;
		if(jsonElement == null || jsonElement.isJsonNull()) {
			updating = null;
		} else {
			updating = new HashMap<>();
			JsonObject variableAsObject = jsonElement.getAsJsonObject();
			for (String variable : variableAsObject.keySet()) {
				String value = variableAsObject.get(variable).getAsString();
				updating.put(variable, value);
			}
		}
		return updating;
	}


	private static DiagramConfiguration buildActivationConfiguration(JsonElement activationElement) {
		if(!activationElement.getAsJsonObject().has("execute")) {
			return buildChoiceActivationConfiguration(activationElement);
		} else {
			return buildOperationConfiguration(activationElement);
		}
	}

	private static Map<String, String> buildParameters(JsonElement jsonElement) {
		Map<String, String> parameters;
		if(jsonElement == null || jsonElement.isJsonNull()) {
			parameters = null;
		} else {
			parameters = new HashMap<>();
			JsonObject parametersAsObject = jsonElement.getAsJsonObject();
			for (String parameter : parametersAsObject.keySet()) {
				String parameterValue = parametersAsObject.get(parameter).getAsString();
				parameters.put(parameter, parameterValue);
			}
		}
		return parameters;
	}

	private static Map<String, String> buildUpdating(JsonElement jsonElement) {
		Map<String, String> updating;
		if(jsonElement == null || jsonElement.isJsonNull()) {
			updating = null;
		} else {
			updating = new HashMap<>();
			JsonObject updateAsObject = jsonElement.getAsJsonObject();
			for (String variable : updateAsObject.keySet()) {
				String value = updateAsObject.get(variable).getAsString();
				updating.put(variable, value);
			}
		}
		return updating;
	}

	private static Object buildProbability(JsonElement jsonElement) {
		Object probability;
		if(jsonElement == null ||jsonElement.isJsonNull()) {
			probability = null;
		} else if(jsonElement.isJsonPrimitive()) {
			probability = jsonElement.getAsString();
		} else {
			JsonObject probabilityObject = jsonElement.getAsJsonObject();
			Map<String, Map<String, String>> probabilityMap = new HashMap<>();
			JsonObject probabilityVariableObject = probabilityObject.getAsJsonObject();

			for (String variable : probabilityVariableObject.keySet()) {
				JsonObject probabilityValueObject = probabilityVariableObject.get(variable).getAsJsonObject();
				Map<String, String> probabilityValueMap = new HashMap<>();
				for (String parameter : probabilityValueObject.keySet()) {
					probabilityValueMap.put(parameter, probabilityValueObject.get(parameter).getAsString());
				}
				probabilityMap.put(variable, probabilityValueMap);
			}
			probability = probabilityMap;
		}
		return probability;
	}

	private static List<String> buildActivation(JsonElement jsonElement) {
		List<String> activations = null;
		if(jsonElement != null && !jsonElement.isJsonNull()) {
			activations = new ArrayList<>();
			if(jsonElement.isJsonArray()) {
				for(int j = 0; j < jsonElement.getAsJsonArray().size(); j++) {
					activations.add(jsonElement.getAsJsonArray().get(j).getAsString());
				}
			} else {
				activations.add(jsonElement.getAsString());
			}
		}
		return activations;
	}

	private static ActivationOperationConfiguration.ActivationKind buildActivationKind(JsonElement jsonElement) {
		ActivationOperationConfiguration.ActivationKind activationKind;
		if(jsonElement == null || jsonElement.isJsonNull() || "multi".equals(jsonElement.getAsString())) {
			activationKind = ActivationOperationConfiguration.ActivationKind.MULTI;
		} else if("single:max".equals(jsonElement.getAsString())) {
			activationKind = ActivationOperationConfiguration.ActivationKind.SINGLE_MAX;
		} else if("single:min".equals(jsonElement.getAsString())) {
			activationKind = ActivationOperationConfiguration.ActivationKind.SINGLE_MIN;
		} else if("single".equals(jsonElement.getAsString())) {
			activationKind = ActivationOperationConfiguration.ActivationKind.SINGLE;
		} else {
			activationKind = ActivationOperationConfiguration.ActivationKind.MULTI;
		}
		return activationKind;
	}

	private static UIListenerConfiguration buildUIListenerConfiguration(JsonElement uiListenerElement) {
		JsonObject uiListenerObject = uiListenerElement.getAsJsonObject();
		String id = uiListenerObject.get("id").getAsString();
		String event = uiListenerObject.get("event").getAsString();
		String predicate = uiListenerObject.get("predicate") == null ? "1=1" : uiListenerObject.get("predicate").getAsString();
		List<String> activating = buildActivation(uiListenerObject.get("activating"));
		return new UIListenerConfiguration(id, event, predicate, activating);
	}

}
