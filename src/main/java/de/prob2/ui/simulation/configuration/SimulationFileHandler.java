package de.prob2.ui.simulation.configuration;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimulationFileHandler {

    public static SimulationConfiguration constructConfigurationFromJSON(File inputFile) throws IOException, JsonSyntaxException {
        Gson gson = new Gson();
        JsonReader reader = new JsonReader(new FileReader(inputFile));
        JsonObject simulationFile = gson.fromJson(reader, JsonObject.class);
        Map<String, ActivationConfiguration> activationConfigurations = buildActivationConfigurations(simulationFile.get("activations"));
        List<OperationConfiguration> operationConfigurations = buildOperationConfigurations(activationConfigurations, simulationFile.get("operationConfigurations"));
        return new SimulationConfiguration(operationConfigurations);
    }

    private static Map<String, ActivationConfiguration> buildActivationConfigurations(JsonElement jsonElement) {
        Map<String, ActivationConfiguration> activationConfigurations = new HashMap<>();
        if(jsonElement != null) {
            JsonObject activationConfigurationsAsObject = jsonElement.getAsJsonObject();
            for (String activationName : activationConfigurationsAsObject.keySet()) {
                JsonObject activationAsObject = activationConfigurationsAsObject.getAsJsonObject(activationName);
                String time = activationAsObject.get("time").getAsString();
                Map<String, String> parameters = buildParameters(activationAsObject.get("parameters"));
                Object probability = buildProbability(activationAsObject.get("probability"));
                activationConfigurations.put(activationName, new ActivationConfiguration(time, parameters, probability));
            }
        }
        return activationConfigurations;
    }

    private static List<OperationConfiguration> buildOperationConfigurations(Map<String, ActivationConfiguration> activationConfigurations, JsonElement jsonElement) {
        JsonArray operationConfigurationsAsArray = jsonElement.getAsJsonArray();
        List<OperationConfiguration> operationConfigurations = new ArrayList<>();
        for(int i = 0; i < operationConfigurationsAsArray.size(); i++) {
            JsonObject jsonObject = (JsonObject) operationConfigurationsAsArray.get(i);
            String opName = jsonObject.get("opName").getAsString();
            String additionalGuards = jsonObject.get("additionalGuards") == null ? null : jsonObject.get("additionalGuards").getAsString();
            int priority = jsonObject.get("priority") == null ? 0 : jsonObject.get("priority").getAsInt();
            Map<String, List<ActivationConfiguration>> activation = buildActivation(activationConfigurations, jsonObject.get("activation"));
            OperationConfiguration.ActivationKind activationKind = buildActivationKind(jsonObject.get("activationKind"));
            Map<String, String> destState = buildDestinationState(jsonObject.get("destState"));
            operationConfigurations.add(new OperationConfiguration(opName, activation, activationKind, additionalGuards, priority, destState));
        }
        return operationConfigurations;
    }

    private static Map<String, String> buildParameters(JsonElement jsonElement) {
        Map<String, String> parameters;
        if(jsonElement == null) {
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

    private static Object buildProbability(JsonElement jsonElement) {
        Object probability;
        if(jsonElement == null) {
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

    private static Map<String, List<ActivationConfiguration>> buildActivation(Map<String, ActivationConfiguration> activationConfigurations, JsonElement jsonElement) {
        Map<String, List<ActivationConfiguration>> activation = null;
        if(jsonElement != null) {
            activation = new HashMap<>();
            JsonObject activationObject = jsonElement.getAsJsonObject();
            if(activationObject != null) {
                for (String key : activationObject.keySet()) {
                    JsonElement activationElement = activationObject.get(key);
                    if(activationElement.isJsonArray()) {
                        List<ActivationConfiguration> activations = new ArrayList<>();
                        for(int j = 0; j < activationElement.getAsJsonArray().size(); j++) {
                            activations.add(buildActivationConfiguration(activationElement.getAsJsonArray().get(j), activationConfigurations));
                        }
                        activation.put(key, activations);
                    } else {
                        activation.put(key, Collections.singletonList(buildActivationConfiguration(activationElement, activationConfigurations)));
                    }
                }
            }
        }
        return activation;
    }

    private static ActivationConfiguration buildActivationConfiguration(JsonElement activationElement, Map<String, ActivationConfiguration> activationConfigurations) {
        if(activationElement.isJsonPrimitive()) {
			String activationConfigurationAsString = activationElement.getAsString();
			if (activationConfigurationAsString.startsWith("$")) {
				return activationConfigurations.get(activationConfigurationAsString.substring(1));
			}
			return new ActivationConfiguration(activationConfigurationAsString, null, null);
		} else {
			JsonObject activationAsObject = activationElement.getAsJsonObject();
			String time = activationAsObject.get("time").getAsString();
			Map<String, String> parameters = buildParameters(activationAsObject.get("parameters"));
			Object probability = buildProbability(activationAsObject.get("probability"));
			return new ActivationConfiguration(time, parameters, probability);
		}
    }


    private static OperationConfiguration.ActivationKind buildActivationKind(JsonElement jsonElement) {
        OperationConfiguration.ActivationKind activationKind = OperationConfiguration.ActivationKind.MULTI;
        if(jsonElement == null || "multi".equals(jsonElement.getAsString())) {
            activationKind = OperationConfiguration.ActivationKind.MULTI;
        } else if("single:max".equals(jsonElement.getAsString())) {
            activationKind = OperationConfiguration.ActivationKind.SINGLE_MAX;
        } else if("single:min".equals(jsonElement.getAsString())) {
            activationKind = OperationConfiguration.ActivationKind.SINGLE_MIN;
        }
        return activationKind;
    }

    private static Map<String, String> buildDestinationState(JsonElement jsonElement) {
        Map<String, String> destState = null;
        if(jsonElement != null) {
			destState = new HashMap<>();
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            for(String key : jsonObject.keySet()) {
				destState.put(key, jsonObject.get(key).getAsString());
            }
        }
        return destState;
    }

}
