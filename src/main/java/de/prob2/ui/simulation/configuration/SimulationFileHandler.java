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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimulationFileHandler {

    public static SimulationConfiguration constructConfigurationFromJSON(File inputFile) throws IOException, JsonSyntaxException {
        Gson gson = new Gson();
        JsonReader reader = new JsonReader(new FileReader(inputFile));
        JsonObject simulationFile = gson.fromJson(reader, JsonObject.class);

        JsonArray operationConfigurationsAsArray = simulationFile.get("operationsConfigurations").getAsJsonArray();
        List<OperationConfiguration> operationConfigurations = buildOperationConfigurations(operationConfigurationsAsArray);

        return new SimulationConfiguration(operationConfigurations);
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

    private static List<OperationConfiguration> buildOperationConfigurations(JsonArray operationConfigurationsAsArray) {
        List<OperationConfiguration> operationConfigurations = new ArrayList<>();
        for(int i = 0; i < operationConfigurationsAsArray.size(); i++) {
            JsonObject jsonObject = (JsonObject) operationConfigurationsAsArray.get(i);
            List<String> opName = new ArrayList<>();
            List<String> probability = new ArrayList<>();
            List<Map<String, Integer>> activation = null;

            // operation name
            if(jsonObject.get("opName").isJsonArray()) {
                for(JsonElement op : jsonObject.get("opName").getAsJsonArray()) {
                    opName.add(op.getAsString());
                }
            } else {
                opName.add(jsonObject.get("opName").getAsString());
            }

            // probability
            if(jsonObject.get("probability").isJsonArray()) {
                for(JsonElement op : jsonObject.get("probability").getAsJsonArray()) {
                    probability.add(op.getAsString());
                }
            } else {
                probability.add(jsonObject.get("probability").getAsString());
            }


            int priority = jsonObject.get("priority") == null ? 0 : jsonObject.get("priority").getAsInt();

            // delay
            if(jsonObject.get("activation") != null) {
                activation = new ArrayList<>();
                if(jsonObject.get("activation").isJsonArray()) {
                    JsonArray activationArray = jsonObject.get("activation").getAsJsonArray();
                    for(JsonElement activationElement : activationArray) {
                        Map<String, Integer> activationMap = new HashMap<>();
                        JsonObject activationObject = activationElement.getAsJsonObject();
                        for (String key : activationObject.keySet()) {
							activationMap.put(key, activationObject.get(key).getAsInt());
                        }
                        activation.add(activationMap);
                    }
                } else {
                    JsonObject activationObject = jsonObject.get("activation").getAsJsonObject();
                    if(activationObject != null) {
                        Map<String, Integer> activationMap = new HashMap<>();
                        for (String key : activationObject.keySet()) {
							activationMap.put(key, activationObject.get(key).getAsInt());
                        }
                        activation.add(activationMap);
                    }
                }

            }

            // variable choices
            List<Map<String, Object>> variableChoices = null;
            if(jsonObject.get("variableChoices") != null) {
                variableChoices = new ArrayList<>();
                if(jsonObject.get("variableChoices") instanceof List) {
                    JsonArray variableChoiceArray = jsonObject.get("variableChoices").getAsJsonArray();
                    for(JsonElement variableChoiceElement : variableChoiceArray) {
                        variableChoices.add(buildVariableChoices(variableChoiceElement.getAsJsonObject()));
                    }
                } else {
                    variableChoices.add(buildVariableChoices(jsonObject.get("variableChoices").getAsJsonObject()));
                }
            }

            operationConfigurations.add(new OperationConfiguration(opName, activation, probability, priority, variableChoices));
        }
        return operationConfigurations;
    }

}
