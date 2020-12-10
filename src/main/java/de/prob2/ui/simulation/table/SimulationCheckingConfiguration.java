package de.prob2.ui.simulation.table;

import de.prob2.ui.simulation.SimulationType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SimulationCheckingConfiguration {

    private SimulationType type;

    private Map<String, Object> information;

    public SimulationCheckingConfiguration(SimulationType type, Map<String, Object> information) {
        this.type = type;
        this.information = information;
    }

    public SimulationType getType() {
        return type;
    }

    public Map<String, Object> getInformation() {
        return information;
    }

    public Object getField(String key) {
        return information.get(key);
    }

    public String getConfiguration() {
        List<String> configurations = new ArrayList<>();
        for(String key : information.keySet()) {
            Object obj = information.get(key);
            configurations.add(key + " : " + obj.toString());
        }
        return String.join(", ", configurations);
    }
}
