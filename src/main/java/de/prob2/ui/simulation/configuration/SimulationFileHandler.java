package de.prob2.ui.simulation.configuration;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;
import de.prob2.ui.simulation.simulators.Activation;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimulationFileHandler {

    public static SimulationConfiguration constructConfigurationFromJSON(File inputFile) throws IOException, JsonSyntaxException {
        Gson gson = new Gson();
        JsonReader reader = new JsonReader(new FileReader(inputFile));
        JsonObject simulationFile = gson.fromJson(reader, JsonObject.class);
        Map<String, ActivationConfiguration> activationConfigurations = buildActivationConfigurations(simulationFile.get("activations"));
        List<TimingConfiguration> timingConfigurations = buildTimingConfigurations(activationConfigurations, simulationFile.get("timingConfigurations"));
        return new SimulationConfiguration(activationConfigurations, timingConfigurations);
    }

    private static Map<String, ActivationConfiguration> buildActivationConfigurations(JsonElement jsonElement) {
        Map<String, ActivationConfiguration> activationConfigurations = new HashMap<>();
        if(jsonElement != null) {
            JsonObject activationConfigurationsAsObject = jsonElement.getAsJsonObject();
            for (String activationName : activationConfigurationsAsObject.keySet()) {
                JsonObject activationAsObject = activationConfigurationsAsObject.getAsJsonObject(activationName);
                int time = activationAsObject.get("time").getAsInt();
                Map<String, String> parameters = buildParameters(activationAsObject.get("parameters"));
                Object probability = buildProbability(activationAsObject.get("probability"));
                activationConfigurations.put(activationName, new ActivationConfiguration(time, parameters, probability));
            }
        }
        return activationConfigurations;
    }

    private static List<TimingConfiguration> buildTimingConfigurations(Map<String, ActivationConfiguration> activationConfigurations, JsonElement jsonElement) {
        JsonArray timingConfigurationsAsArray = jsonElement.getAsJsonArray();
        List<TimingConfiguration> timingConfigurations = new ArrayList<>();
        for(int i = 0; i < timingConfigurationsAsArray.size(); i++) {
            JsonObject jsonObject = (JsonObject) timingConfigurationsAsArray.get(i);
            String opName = jsonObject.get("opName").getAsString();
            String additionalGuards = jsonObject.get("additionalGuards") == null ? null : jsonObject.get("additionalGuards").getAsString();
            int priority = jsonObject.get("priority") == null ? 0 : jsonObject.get("priority").getAsInt();
            Map<String, List<ActivationConfiguration>> activation = buildActivation(activationConfigurations, jsonObject.get("activation"));
            TimingConfiguration.ActivationKind activationKind = buildActivationKind(jsonObject.get("activationKind"));
            Map<String, String> variableChoices = buildVariableChoices(jsonObject.get("variableChoices"));
            timingConfigurations.add(new TimingConfiguration(opName, activation, activationKind, additionalGuards, priority, variableChoices));
        }
        return timingConfigurations;
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
            Map<String, Object> probabilityMap = new HashMap<>();
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
                            String activationConfigurationAsString = activationElement.getAsJsonArray().get(j).getAsString();
                            activations.add(buildActivationConfiguration(activationConfigurationAsString, activationConfigurations));
                        }
                        activation.put(key, activations);
                    } else {
                        // TODO: Implement explicit definition of activation with object
                        String activationConfigurationAsString = activationElement.getAsString();
                        activation.put(key, Arrays.asList(buildActivationConfiguration(activationConfigurationAsString, activationConfigurations)));
                    }
                }
            }
        }
        return activation;
    }

    private static ActivationConfiguration buildActivationConfiguration(String activationConfigurationAsString, Map<String, ActivationConfiguration> activationConfigurations) {
        if (activationConfigurationAsString.startsWith("$")) {
            return activationConfigurations.get(activationConfigurationAsString.substring(1));
        }
        return new ActivationConfiguration(Integer.parseInt(activationConfigurationAsString), null, null);
    }


    private static TimingConfiguration.ActivationKind buildActivationKind(JsonElement jsonElement) {
        TimingConfiguration.ActivationKind activationKind = TimingConfiguration.ActivationKind.MULTI;
        if(jsonElement == null || "multi".equals(jsonElement.getAsString())) {
            activationKind = TimingConfiguration.ActivationKind.MULTI;
        } else if("single:max".equals(jsonElement.getAsString())) {
            activationKind = TimingConfiguration.ActivationKind.SINGLE_MAX;
        } else if("single:min".equals(jsonElement.getAsString())) {
            activationKind = TimingConfiguration.ActivationKind.SINGLE_MIN;
        }
        return activationKind;
    }

    private static Map<String, String> buildVariableChoices(JsonElement jsonElement) {
        Map<String, String> variableChoices = null;
        if(jsonElement != null) {
            variableChoices = new HashMap<>();
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            for(String key : jsonObject.keySet()) {
                variableChoices.put(key, jsonObject.get(key).getAsString());
            }
        }
        return variableChoices;
    }

}