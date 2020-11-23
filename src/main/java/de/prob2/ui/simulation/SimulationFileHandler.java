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
import java.util.List;

public class SimulationFileHandler {

    public static SimulationConfiguration constructConfigurationFromJSON(File inputFile) throws IOException, JsonSyntaxException {
        Gson gson = new Gson();
        JsonReader reader = new JsonReader(new FileReader(inputFile));
        JsonObject simulationFile = gson.fromJson(reader, JsonObject.class);

        int time = simulationFile.get("time").getAsInt();

        JsonArray operationConfigurationsAsArray = simulationFile.get("operationConfigurations").getAsJsonArray();
        List<OperationConfiguration> operationConfigurations = new ArrayList<>();

        for(int i = 0; i < operationConfigurationsAsArray.size(); i++) {
            JsonObject jsonObject = (JsonObject) operationConfigurationsAsArray.get(i);
            String opName = jsonObject.get("opName").getAsString();
            int opTime = jsonObject.get("time").getAsInt();
            float probability = jsonObject.get("probability").getAsFloat();
            operationConfigurations.add(new OperationConfiguration(opName, opTime, probability));
        }

        return new SimulationConfiguration(time, operationConfigurations);
    }

}
