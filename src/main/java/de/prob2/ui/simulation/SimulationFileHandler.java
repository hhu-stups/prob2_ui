package de.prob2.ui.simulation;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
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

        List<VariableChoice> setupConfigurations = simulationFile.get("setupConfigurations") == null ? null : buildVariableChoices(simulationFile.get("setupConfigurations").getAsJsonArray());
        List<VariableChoice> initialisationConfigurations = simulationFile.get("initialisationConfigurations") == null ? null : buildVariableChoices(simulationFile.get("initialisationConfigurations").getAsJsonArray());


        JsonArray operationConfigurationsAsArray = simulationFile.get("operationsConfigurations").getAsJsonArray();
        List<OperationConfiguration> operationConfigurations = buildOperationConfigurations(operationConfigurationsAsArray);

        return new SimulationConfiguration(setupConfigurations, initialisationConfigurations, operationConfigurations);
    }

    private static List<VariableChoice> buildVariableChoices(JsonArray jsonArray) {
        List<VariableChoice> variableChoices = new ArrayList<>();
        for(int i = 0; i < jsonArray.size(); i++) {
            JsonObject jsonObject = (JsonObject) jsonArray.get(i);
            JsonArray choiceAsArray = jsonObject.get("choice").getAsJsonArray();
            variableChoices.add(buildVariableChoice(choiceAsArray));
        }
        return variableChoices;
    }

    private static VariableChoice buildVariableChoice(JsonArray jsonArray) {
        List<VariableConfiguration> variableConfigurations = new ArrayList<>();
        for(int i = 0; i < jsonArray.size(); i++) {
            JsonObject jsonObject = (JsonObject) jsonArray.get(i);
            variableConfigurations.add(buildVariableConfiguration(jsonObject));
        }
        return new VariableChoice(variableConfigurations);
    }

    private static VariableConfiguration buildVariableConfiguration(JsonObject jsonObject) {
        JsonObject valuesObject = jsonObject.get("values").getAsJsonObject();
        String probability = jsonObject.get("probability").getAsString();

        Map<String, String> values = new HashMap<>();
        for(String key : valuesObject.keySet()) {
            values.put(key, valuesObject.get(key).getAsString());
        }

        return new VariableConfiguration(values, probability);
    }

    private static List<OperationConfiguration> buildOperationConfigurations(JsonArray operationConfigurationsAsArray) {
        List<OperationConfiguration> operationConfigurations = new ArrayList<>();
        for(int i = 0; i < operationConfigurationsAsArray.size(); i++) {
            JsonObject jsonObject = (JsonObject) operationConfigurationsAsArray.get(i);
            String opName = jsonObject.get("opName").getAsString();
            String probability = jsonObject.get("probability") == null ? "-1.0f" : jsonObject.get("probability").getAsString();
            int priority = jsonObject.get("priority") == null ? 0 : jsonObject.get("priority").getAsInt();
            int opTime =  jsonObject.get("time") == null ? -1 : jsonObject.get("time").getAsInt();
            List<VariableChoice> variableChoices = jsonObject.get("variableChoices") == null ? null : buildVariableChoices(jsonObject.get("variableChoices").getAsJsonArray());
            operationConfigurations.add(new OperationConfiguration(opName, opTime, probability, priority, variableChoices));
        }
        return operationConfigurations;
    }

}
