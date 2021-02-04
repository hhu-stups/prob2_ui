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
        List<ActivationConfiguration> activationConfigurations = buildActivationConfigurations(simulationFile.get("activations"));
        return new SimulationConfiguration(activationConfigurations);
    }

    private static List<ActivationConfiguration> buildActivationConfigurations(JsonElement jsonElement) {
        List<ActivationConfiguration> activationConfigurations = new ArrayList<>();
        JsonArray activationConfigurationsAsArray = jsonElement.getAsJsonArray();
        for (JsonElement activationElement : activationConfigurationsAsArray) {
            ActivationConfiguration activationConfiguration = buildActivationConfiguration(activationElement);
            activationConfigurations.add(activationConfiguration);
        }
        return activationConfigurations;
    }

    private static ActivationConfiguration buildActivationConfiguration(JsonElement activationElement) {
        if(!activationElement.getAsJsonObject().has("op")) {
            JsonObject activationAsObject = activationElement.getAsJsonObject();
            String id = activationAsObject.get("id").getAsString();
            JsonArray activationsArray = activationAsObject.getAsJsonArray("activations");
            JsonArray probabilityArray = activationAsObject.getAsJsonArray("probability");
            assert(activationsArray.size() == probabilityArray.size());
            List<String> activations = new ArrayList<>();
            List<String> probability = new ArrayList<>();
            for(int i = 0; i < activationsArray.size(); i++) {
                activations.add(activationsArray.get(i).getAsString());
                probability.add(probabilityArray.get(i).getAsString());
            }
            return new ActivationChoiceConfiguration(id, activations, probability);
        } else {
            JsonObject activationAsObject = activationElement.getAsJsonObject();
            String id = activationAsObject.get("id").getAsString();
            String opName = activationAsObject.get("op").getAsString();

            int priority = activationAsObject.get("priority") == null ? 0 : activationAsObject.get("priority").getAsInt();
            List<String> activations = buildActivation(activationAsObject.get("activations"));

            String time = activationAsObject.get("time") == null ? "0" : activationAsObject.get("time").getAsString();
            String additionalGuards = activationAsObject.get("additionalGuards") == null ? null : activationAsObject.get("additionalGuards").getAsString();
            ActivationOperationConfiguration.ActivationKind activationKind = buildActivationKind(activationAsObject.get("activationKind"));
            Map<String, String> fixedVariables = buildParameters(activationAsObject.get("fixedVariables"));
            Object probabilisticVariables = buildProbability(activationAsObject.get("probabilisticVariables"));
            return new ActivationOperationConfiguration(id, opName, time, priority, additionalGuards, activationKind, fixedVariables, probabilisticVariables, activations);
        }
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

    private static List<String> buildActivation(JsonElement jsonElement) {
        List<String> activations = null;
        if(jsonElement != null) {
            activations = new ArrayList<>();
            if(jsonElement.isJsonArray()) {
                for(int j = 0; j < jsonElement.getAsJsonArray().size(); j++) {
                    activations.add(jsonElement.getAsJsonArray().get(j).getAsString());
                }
            } else {
                activations.addAll(Collections.singletonList(jsonElement.getAsString()));
            }
        }
        return activations;
    }

    private static ActivationOperationConfiguration.ActivationKind buildActivationKind(JsonElement jsonElement) {
        ActivationOperationConfiguration.ActivationKind activationKind = ActivationOperationConfiguration.ActivationKind.MULTI;
        if(jsonElement == null || "multi".equals(jsonElement.getAsString())) {
            activationKind = ActivationOperationConfiguration.ActivationKind.MULTI;
        } else if("single:max".equals(jsonElement.getAsString())) {
            activationKind = ActivationOperationConfiguration.ActivationKind.SINGLE_MAX;
        } else if("single:min".equals(jsonElement.getAsString())) {
            activationKind = ActivationOperationConfiguration.ActivationKind.SINGLE_MIN;
        }
        return activationKind;
    }

}
