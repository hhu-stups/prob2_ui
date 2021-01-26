package de.prob2.ui.simulation;

import de.prob2.ui.animation.tracereplay.ReplayTrace;
import de.prob2.ui.simulation.choice.SimulationType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SimulationCheckingConfiguration {

    private final SimulationType type;

    private final Map<String, Object> information;

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

    public boolean containsField(String key) {
        return information.containsKey(key);
    }

    public Object getField(String key) {
        return information.get(key);
    }

    public String getConfiguration() {
        List<String> configurations = new ArrayList<>();
        for(String key : information.keySet()) {
            Object obj = information.get(key);
            if(obj instanceof ReplayTrace) {
                configurations.add(key + " : " + ((ReplayTrace) obj).getName());
            } else {
                configurations.add(key + " : " + obj.toString());
            }
        }
        if(!configurations.isEmpty()) {
            return String.join(", ", configurations);
        }
        return type.name();
    }
}
