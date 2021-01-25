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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimulationFileHandler {

    public static SimulationConfiguration constructConfigurationFromJSON(File inputFile) throws IOException, JsonSyntaxException {
        Gson gson = new Gson();
        JsonReader reader = new JsonReader(new FileReader(inputFile));
        JsonObject simulationFile = gson.fromJson(reader, JsonObject.class);

        Map<String, ActivationConfiguration> activationConfigurations = new HashMap<>();
        if(simulationFile.get("activations") != null) {
            JsonObject activationConfigurationsAsObject = simulationFile.get("activations").getAsJsonObject();
            activationConfigurations = buildActivationConfigurations(activationConfigurationsAsObject);
        }

        JsonArray timingConfigurationsAsArray = simulationFile.get("timingConfigurations").getAsJsonArray();
        List<TimingConfiguration> timingConfigurations = buildTimingConfigurations(activationConfigurations, timingConfigurationsAsArray);
        return new SimulationConfiguration(activationConfigurations, timingConfigurations);
    }

    private static Map<String, Object> buildVariableChoices(JsonObject jsonObject) {
        Map<String, Object> values = new HashMap<>();
        for(String key : jsonObject.keySet()) {
            values.put(key, buildVariableExpression(jsonObject.get(key)));
        }
        return values;
    }

    private static Object buildVariableExpression(JsonElement jsonElement) {
        if(jsonElement instanceof JsonArray) {
            List<String> expressions = new ArrayList<>();
            JsonArray jsonArray = jsonElement.getAsJsonArray();
            for(int i = 0; i < jsonArray.size(); i++) {
                expressions.add(jsonArray.get(i).getAsString());
            }
            return expressions;
        } else {
            return jsonElement.getAsString();
        }
    }

    private static Map<String, ActivationConfiguration> buildActivationConfigurations(JsonObject activationConfigurationsAsObject) {
        Map<String, ActivationConfiguration> activationConfigurations = new HashMap<>();

        for(String activationName : activationConfigurationsAsObject.keySet()) {
            JsonObject activationAsObject = activationConfigurationsAsObject.getAsJsonObject(activationName);
            int time = activationAsObject.get("time").getAsInt();
            JsonElement probabilityAsElement = activationAsObject.get("probability");
            if(probabilityAsElement.isJsonPrimitive()) {
                activationConfigurations.put(activationName, new ActivationConfiguration(time, probabilityAsElement.getAsString()));
            } else {
                JsonObject probabilityObject = probabilityAsElement.getAsJsonObject();
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
                activationConfigurations.put(activationName, new ActivationConfiguration(time, probabilityMap));
            }
        }
        return activationConfigurations;
    }

    private static List<TimingConfiguration> buildTimingConfigurations(Map<String, ActivationConfiguration> activationConfigurations, JsonArray timingConfigurationsAsArray) {
        List<TimingConfiguration> timingConfigurations = new ArrayList<>();
        for(int i = 0; i < timingConfigurationsAsArray.size(); i++) {
            JsonObject jsonObject = (JsonObject) timingConfigurationsAsArray.get(i);
            String opName = jsonObject.get("opName").getAsString();
            Map<String, ActivationConfiguration> activation = null;
            String additionalGuards = jsonObject.get("additionalGuards") == null ? null : jsonObject.get("additionalGuards").getAsString();
            int priority = jsonObject.get("priority") == null ? 0 : jsonObject.get("priority").getAsInt();

            // activation
            if(jsonObject.get("activation") != null) {
                activation = new HashMap<>();
                JsonObject activationObject = jsonObject.get("activation").getAsJsonObject();
                if(activationObject != null) {
                    for (String key : activationObject.keySet()) {
                        // TODO: Implement explicit definition of activation with object
                        String activationConfigurationAsString = activationObject.get(key).getAsString();
                        if(activationConfigurationAsString.startsWith("$")) {
                            activation.put(key, activationConfigurations.get(activationConfigurationAsString.substring(1)));
                        } else {
                            activation.put(key, new ActivationConfiguration(Integer.parseInt(activationConfigurationAsString), null));
                        }

                    }
                }
            }

            TimingConfiguration.ActivationKind activationKind = TimingConfiguration.ActivationKind.MULTI;
            if(jsonObject.get("activationKind") == null || "multi".equals(jsonObject.get("activationKind").getAsString())) {
                activationKind = TimingConfiguration.ActivationKind.MULTI;
            } else if("single:max".equals(jsonObject.get("activationKind").getAsString())) {
                activationKind = TimingConfiguration.ActivationKind.SINGLE_MAX;
            } else if("single:min".equals(jsonObject.get("activationKind").getAsString())) {
                activationKind = TimingConfiguration.ActivationKind.SINGLE_MIN;
            }

            // variable choices
            Map<String, Object> variableChoices = null;
            if(jsonObject.get("variableChoices") != null) {
                variableChoices = buildVariableChoices(jsonObject.get("variableChoices").getAsJsonObject());
            }

            timingConfigurations.add(new TimingConfiguration(opName, activation, activationKind, additionalGuards, priority, variableChoices));
        }
        return timingConfigurations;
    }


}
